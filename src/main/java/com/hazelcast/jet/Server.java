package com.hazelcast.jet;

public class Server {


    public static final String MAP_NAME = "lines";

    private static final int NUM_WORDS = 1_000_000;
    private static final int NUM_DISTINCT = 10_000;
    public static final int WORDS_PER_LINE = 20;

    public static void main(String[] args) {
        JetInstance jet = Jet.newJetInstance();
        IMapJet<Integer, String> map = jet.getMap(MAP_NAME);
        System.out.println("Populating map");
        populateMap(map);
        System.out.println("Populated map with " + NUM_WORDS);

    }

    private static void populateMap(IMapJet<Integer, String> map) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < NUM_WORDS; i++) {
            sb.append(i % NUM_DISTINCT);
            if (i % WORDS_PER_LINE == 0) {
                map.set(i / WORDS_PER_LINE, sb.toString());
                sb.setLength(0);
            } else {
                sb.append(" ");
            }
        }
    }
}
