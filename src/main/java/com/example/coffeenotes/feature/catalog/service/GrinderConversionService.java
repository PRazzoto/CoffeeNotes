package com.example.coffeenotes.feature.catalog.service;

import com.example.coffeenotes.api.dto.grinder.GrinderCatalogItemDTO;
import com.example.coffeenotes.api.dto.grinder.GrinderConversionRequestDTO;
import com.example.coffeenotes.api.dto.grinder.GrinderConversionResponseDTO;
import com.example.coffeenotes.api.dto.grinder.GrinderSettingDTO;
import com.example.coffeenotes.api.dto.grinder.GrinderUnitDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GrinderConversionService {
    private static final String GRINDERS_RESOURCE = "grinder_conversion/data/beean_widget_grinders.json";
    private static final String REFERENCE_RESOURCE = "grinder_conversion/data/free_grinders_reference_conversion.json";

    private final Map<String, GrinderMeta> grindersById;
    private final Map<String, GrinderCurves> curvesByGrinderId;

    public GrinderConversionService(ObjectMapper objectMapper) {
        try {
            JsonNode grinderCatalog = readTreeFromResource(objectMapper, GRINDERS_RESOURCE);
            JsonNode referenceDataset = readTreeFromResource(objectMapper, REFERENCE_RESOURCE);

            Map<String, GrinderMeta> parsedGrinders = parseGrinders(grinderCatalog);
            Map<String, GrinderCurves> parsedCurves = parseCurves(referenceDataset);

            this.grindersById = Collections.unmodifiableMap(parsedGrinders);
            this.curvesByGrinderId = Collections.unmodifiableMap(parsedCurves);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to load grinder conversion dataset.", ex);
        }
    }

    public List<GrinderCatalogItemDTO> listSupportedGrinders() {
        List<GrinderCatalogItemDTO> out = new ArrayList<>();
        for (Map.Entry<String, GrinderCurves> entry : curvesByGrinderId.entrySet()) {
            GrinderMeta grinder = grindersById.get(entry.getKey());
            if (grinder == null) {
                continue;
            }
            GrinderCatalogItemDTO dto = new GrinderCatalogItemDTO();
            dto.setId(grinder.id);
            dto.setName(grinder.name);
            dto.setMake(grinder.make);
            dto.setModel(grinder.model);
            dto.setTier(grinder.tier);

            List<GrinderUnitDTO> units = new ArrayList<>();
            for (UnitMeta unit : grinder.units) {
                GrinderUnitDTO unitDTO = new GrinderUnitDTO();
                unitDTO.setLabel(unit.label);
                unitDTO.setMaximum(unit.maximum);
                units.add(unitDTO);
            }
            dto.setUnits(units);
            out.add(dto);
        }

        out.sort(Comparator.comparing(GrinderCatalogItemDTO::getName));
        return out;
    }

    public GrinderConversionResponseDTO convert(GrinderConversionRequestDTO request) {
        if (request == null || isBlank(request.getSourceGrinderId()) || isBlank(request.getTargetGrinderId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Required fields missing.");
        }

        String sourceId = request.getSourceGrinderId().trim();
        String targetId = request.getTargetGrinderId().trim();

        GrinderMeta source = grindersById.get(sourceId);
        GrinderMeta target = grindersById.get(targetId);
        if (source == null || target == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Grinder not found.");
        }

        GrinderCurves sourceCurves = curvesByGrinderId.get(sourceId);
        GrinderCurves targetCurves = curvesByGrinderId.get(targetId);
        if (sourceCurves == null || targetCurves == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Grinder conversion not supported.");
        }

        int[] sourceValues = source.extractValues(request.getSourceSetting());
        int sourceFlatRaw = flattenValues(sourceValues, source.bases);
        int sourceFlat = clamp(sourceFlatRaw, source.flatStart, source.flatEnd);
        sourceValues = unflattenValues(sourceFlat, source.bases);

        double referenceFlat;
        int targetFlat;
        int[] targetValues;

        if (sourceId.equals(targetId)) {
            referenceFlat = sourceFlat;
            targetFlat = sourceFlat;
            targetValues = sourceValues;
        } else {
            referenceFlat = interpolate(sourceFlat, sourceCurves.toReferencePoints);
            double targetFlatEstimate = interpolate(referenceFlat, targetCurves.fromReferencePoints);
            targetFlat = clamp((int) Math.round(targetFlatEstimate), target.flatStart, target.flatEnd);
            targetValues = unflattenValues(targetFlat, target.bases);
        }

        GrinderConversionResponseDTO dto = new GrinderConversionResponseDTO();
        dto.setSourceGrinderId(sourceId);
        dto.setTargetGrinderId(targetId);
        dto.setSourceSetting(source.toSettingDTO(sourceValues));
        dto.setTargetSetting(target.toSettingDTO(targetValues));
        dto.setSourceFlat(sourceFlat);
        dto.setTargetFlat(targetFlat);
        dto.setReferenceFlatEstimated(referenceFlat);
        dto.setConfidence(combineConfidence(sourceCurves.confidence, targetCurves.confidence));
        return dto;
    }

    private static JsonNode readTreeFromResource(ObjectMapper objectMapper, String resourcePath) throws IOException {
        ClassPathResource resource = new ClassPathResource(resourcePath);
        try (InputStream inputStream = resource.getInputStream()) {
            return objectMapper.readTree(inputStream);
        }
    }

    private static Map<String, GrinderMeta> parseGrinders(JsonNode catalogRoot) {
        Map<String, GrinderMeta> out = new HashMap<>();
        for (JsonNode grinderNode : catalogRoot) {
            String id = grinderNode.path("id").asText(null);
            if (isBlank(id)) {
                continue;
            }

            List<UnitMeta> units = new ArrayList<>();
            List<Integer> basesList = new ArrayList<>();
            for (JsonNode unitNode : grinderNode.path("units")) {
                String label = unitNode.path("label").asText();
                int maximum = unitNode.path("maximum").asInt();
                units.add(new UnitMeta(label, maximum));
                basesList.add(maximum + 1);
            }
            if (units.isEmpty()) {
                continue;
            }

            int[] bases = basesList.stream().mapToInt(Integer::intValue).toArray();
            int[] start = toIntArray(grinderNode.path("range").path("grinder").path("start"), units.size());
            int[] end = toIntArray(grinderNode.path("range").path("grinder").path("end"), units.size());
            int flatStart = flattenValues(start, bases);
            int flatEnd = flattenValues(end, bases);

            GrinderMeta meta = new GrinderMeta(
                    id,
                    grinderNode.path("name").asText(),
                    grinderNode.path("make").asText(),
                    grinderNode.path("model").asText(),
                    grinderNode.path("tier").asText(),
                    units,
                    bases,
                    flatStart,
                    flatEnd
            );
            out.put(id, meta);
        }
        return out;
    }

    private static Map<String, GrinderCurves> parseCurves(JsonNode datasetRoot) {
        Map<String, GrinderCurves> out = new HashMap<>();
        for (JsonNode grinderNode : datasetRoot.path("grinders")) {
            String grinderId = grinderNode.path("grinder_id").asText(null);
            if (isBlank(grinderId)) {
                continue;
            }
            JsonNode toReference = grinderNode.get("to_reference");
            JsonNode fromReference = grinderNode.get("from_reference");
            if (toReference == null || fromReference == null || toReference.isNull() || fromReference.isNull()) {
                continue;
            }

            List<Point> toReferencePoints = parsePoints(toReference, "target_flat_raw");
            List<Point> fromReferencePoints = parsePoints(fromReference, "target_flat_clamped");
            if (toReferencePoints.isEmpty() || fromReferencePoints.isEmpty()) {
                continue;
            }

            GrinderCurves curves = new GrinderCurves(
                    grinderNode.path("confidence").asText("low"),
                    toReferencePoints,
                    fromReferencePoints
            );
            out.put(grinderId, curves);
        }
        return out;
    }

    private static List<Point> parsePoints(JsonNode directionNode, String yField) {
        List<Point> points = new ArrayList<>();
        for (JsonNode mappingNode : directionNode.path("mappings")) {
            double x = mappingNode.path("source_flat").asDouble(Double.NaN);
            double y = mappingNode.path(yField).asDouble(Double.NaN);
            if (Double.isNaN(x) || Double.isNaN(y)) {
                continue;
            }
            points.add(new Point(x, y));
        }
        points.sort(Comparator.comparingDouble(p -> p.x));
        return points;
    }

    private static int[] toIntArray(JsonNode arrayNode, int expectedSize) {
        int[] out = new int[expectedSize];
        for (int i = 0; i < expectedSize; i++) {
            out[i] = arrayNode.path(i).asInt(0);
        }
        return out;
    }

    private static int flattenValues(int[] values, int[] bases) {
        int total = 0;
        for (int i = 0; i < values.length; i++) {
            int multiplier = 1;
            for (int j = i + 1; j < bases.length; j++) {
                multiplier *= bases[j];
            }
            total += values[i] * multiplier;
        }
        return total;
    }

    private static int[] unflattenValues(int flatValue, int[] bases) {
        int[] values = new int[bases.length];
        int remainder = flatValue;
        for (int i = bases.length - 1; i >= 0; i--) {
            values[i] = remainder % bases[i];
            remainder /= bases[i];
        }
        return values;
    }

    private static double interpolate(double input, List<Point> points) {
        if (points.size() == 1) {
            return points.get(0).y;
        }
        if (input <= points.get(0).x) {
            return points.get(0).y;
        }
        Point last = points.get(points.size() - 1);
        if (input >= last.x) {
            return last.y;
        }

        for (int i = 1; i < points.size(); i++) {
            Point left = points.get(i - 1);
            Point right = points.get(i);
            if (input < left.x || input > right.x) {
                continue;
            }
            if (right.x == left.x) {
                return left.y;
            }
            double ratio = (input - left.x) / (right.x - left.x);
            return left.y + ratio * (right.y - left.y);
        }
        return last.y;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String combineConfidence(String sourceConfidence, String targetConfidence) {
        int sourceRank = confidenceRank(sourceConfidence);
        int targetRank = confidenceRank(targetConfidence);
        int worst = Math.min(sourceRank, targetRank);
        if (worst == 3) {
            return "high";
        }
        if (worst == 2) {
            return "medium";
        }
        return "low";
    }

    private static int confidenceRank(String confidence) {
        if ("high".equalsIgnoreCase(confidence)) {
            return 3;
        }
        if ("medium".equalsIgnoreCase(confidence)) {
            return 2;
        }
        return 1;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static final class UnitMeta {
        private final String label;
        private final int maximum;

        private UnitMeta(String label, int maximum) {
            this.label = label;
            this.maximum = maximum;
        }
    }

    private static final class GrinderMeta {
        private final String id;
        private final String name;
        private final String make;
        private final String model;
        private final String tier;
        private final List<UnitMeta> units;
        private final int[] bases;
        private final int flatStart;
        private final int flatEnd;

        private GrinderMeta(
                String id,
                String name,
                String make,
                String model,
                String tier,
                List<UnitMeta> units,
                int[] bases,
                int flatStart,
                int flatEnd
        ) {
            this.id = id;
            this.name = name;
            this.make = make;
            this.model = model;
            this.tier = tier;
            this.units = units;
            this.bases = bases;
            this.flatStart = flatStart;
            this.flatEnd = flatEnd;
        }

        private int[] extractValues(GrinderSettingDTO input) {
            int[] values = new int[units.size()];
            for (int i = 0; i < units.size(); i++) {
                String unit = units.get(i).label;
                values[i] = getSettingValue(input, unit);
            }
            return values;
        }

        private GrinderSettingDTO toSettingDTO(int[] values) {
            GrinderSettingDTO setting = new GrinderSettingDTO();
            for (int i = 0; i < units.size(); i++) {
                String unit = units.get(i).label;
                int value = values[i];
                if ("rotation".equals(unit)) {
                    setting.setRotation(value);
                } else if ("number".equals(unit)) {
                    setting.setNumber(value);
                } else if ("click".equals(unit)) {
                    setting.setClick(value);
                }
            }
            return setting;
        }
    }

    private static int getSettingValue(GrinderSettingDTO input, String unit) {
        if (input == null) {
            return 0;
        }
        if ("rotation".equals(unit)) {
            return input.getRotation() != null ? input.getRotation() : 0;
        }
        if ("number".equals(unit)) {
            return input.getNumber() != null ? input.getNumber() : 0;
        }
        if ("click".equals(unit)) {
            return input.getClick() != null ? input.getClick() : 0;
        }
        return 0;
    }

    private static final class GrinderCurves {
        private final String confidence;
        private final List<Point> toReferencePoints;
        private final List<Point> fromReferencePoints;

        private GrinderCurves(
                String confidence,
                List<Point> toReferencePoints,
                List<Point> fromReferencePoints
        ) {
            this.confidence = confidence;
            this.toReferencePoints = toReferencePoints;
            this.fromReferencePoints = fromReferencePoints;
        }
    }

    private static final class Point {
        private final double x;
        private final double y;

        private Point(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }
}
