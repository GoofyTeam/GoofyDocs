package com.goofy.GoofyDocs.duplication;

public enum HashingAlgorithm {
    SHA1("SHA-1"),
    SHA256("SHA-256"),
    BLAKE3("BLAKE3");

    private final String algorithmName;

    HashingAlgorithm(String algorithmName) {
        this.algorithmName = algorithmName;
    }

    public String getAlgorithmName() {
        return algorithmName;
    }
}
