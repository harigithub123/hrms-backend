package com.hrms.offers;

import com.hrms.offers.dto.OfferDto;

import java.util.List;

final class OfferCsvExporter {

    private OfferCsvExporter() {}

    static String toCsv(List<OfferDto> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",",
                "id",
                "candidateName",
                "candidateEmail",
                "candidateMobile",
                "employeeType",
                "status",
                "department",
                "designation",
                "joiningDate",
                "offerReleaseDate",
                "probationMonths",
                "employeeId"
        )).append("\n");

        for (OfferDto o : rows) {
            sb.append(csv(o.id()))
                    .append(',').append(csv(o.candidateName()))
                    .append(',').append(csv(o.candidateEmail()))
                    .append(',').append(csv(o.candidateMobile()))
                    .append(',').append(csv(o.employeeType()))
                    .append(',').append(csv(o.status() != null ? o.status().name() : null))
                    .append(',').append(csv(o.departmentName()))
                    .append(',').append(csv(o.designationName()))
                    .append(',').append(csv(o.joiningDate() != null ? o.joiningDate().toString() : null))
                    .append(',').append(csv(o.offerReleaseDate() != null ? o.offerReleaseDate().toString() : null))
                    .append(',').append(csv(o.probationPeriodMonths()))
                    .append(',').append(csv(o.employeeId()))
                    .append("\n");
        }
        return sb.toString();
    }

    private static String csv(Object v) {
        if (v == null) return "";
        String s = String.valueOf(v);
        boolean needsQuotes = s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r");
        if (!needsQuotes) return s;
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }
}

