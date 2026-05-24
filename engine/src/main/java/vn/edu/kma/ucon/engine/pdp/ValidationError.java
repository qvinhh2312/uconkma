package vn.edu.kma.ucon.engine.pdp;

public record ValidationError(String code, String location, String message) {
}
