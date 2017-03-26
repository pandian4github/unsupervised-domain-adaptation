package edu.utexas.nlp.hw3;

import java.io.*;
import java.util.*;

/**
 * Created by pandian on 3/25/17.
 */
public class Preprocessor {
    private String wsjSeedSetFilePath; // wsj_00_22.conllx
    private String wsjTestSetFilePath; // wsj_23.conllx
    private String brownCorpusBasePath; // brown-conllx
    private String executionDirectory;
    private Map<String, Integer> brown90PercentCounts;

    private List<Integer> wsjSeedSetSizes;
    private List<Integer> brownSelfTrainingSizes;

    private String currentTime() {
        return new Date().toString() + " ";
    }

    public Preprocessor(String wsjSeedSetFilePath, String wsjTestSetFilePath, String brownCorpusBasePath, String executionDirectory) {
        this.wsjSeedSetFilePath = wsjSeedSetFilePath;
        this.wsjTestSetFilePath = wsjTestSetFilePath;
        this.brownCorpusBasePath = brownCorpusBasePath;
        this.executionDirectory = executionDirectory;

        wsjSeedSetSizes = new ArrayList<>(Arrays.asList(1000, 2000, 3000, 4000, 5000, 7000, 10000, 13000, 16000, 20000, 25000, 30000, 35000));
        brownSelfTrainingSizes = new ArrayList<>(Arrays.asList(1000, 2000, 3000, 4000, 5000, 7000, 10000, 13000, 17000, 21000));

        createDirectory();
        brown90PercentCounts = new HashMap<>();
    }

    private void readFromFile(String fileName, StringBuilder builder) {
        String line;
        try {
            FileReader fileReader = new FileReader(fileName);
            BufferedReader bufferedReader = new BufferedReader(fileReader);

            while ((line = bufferedReader.readLine()) != null) {
                builder.append(line).append("\n");
            }
            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setBrown90PercentCounts(Map<String, Integer> brown90PercentCounts) {
        this.brown90PercentCounts.putAll(brown90PercentCounts);
    }

    private void createDirectory() {
        File directory = new File(this.executionDirectory);
        if (directory.mkdir()) {
            System.out.println(currentTime() + "Directory " + directory + " created successfully. ");
        } else {
            System.out.println(currentTime() + "Directory creation failed for " + directory + " !");
        }
    }

    /*
      Create separate conllx files with 1000, 2000, 3000 etc number of sentences from the wsj seed set
     */
    public void createWsjSeedFiles() {
        createIncrementalSeedFiles(wsjSeedSetFilePath, wsjSeedSetSizes, "wsj_seed_self_training_");
    }

    public void copyWsjTrainingTestFilesIntoExecutionDir() {
        StringBuilder trainingBuilder = new StringBuilder();
        StringBuilder testBuilder = new StringBuilder();

        String wsjTrainingFileName = executionDirectory + "/" + "wsj_training.conllx";
        String wsjTestFileName = executionDirectory + "/" + "wsj_test.conllx";

        readFromFile(wsjSeedSetFilePath, trainingBuilder);
        readFromFile(wsjTestSetFilePath, testBuilder);

        writeToFile(wsjTrainingFileName, trainingBuilder);
        writeToFile(wsjTestFileName, testBuilder);
    }

    /*
     Create separate files with 1000, 2000, 3000 etc number of sentences from the brown self-training set
     */
    private void createBrownTrainingFiles() {
        createIncrementalSeedFiles(this.executionDirectory + "/" + "brown_training.conllx", brownSelfTrainingSizes, "brown_seed_self_training_");
    }

    /*
      Generic method to create the incremental sub files with increasing number of sentences from the seed/self-training set
     */
    private void createIncrementalSeedFiles(String mainFile, List<Integer> incrementalSizes, String subFilePrefix) {
        String line;

        int index = 0;
        int count = 0;
        boolean sentenceStarted = false;
        StringBuilder stringBuilder = new StringBuilder();

        try {
            FileReader fileReader = new FileReader(mainFile);
            BufferedReader bufferedReader = new BufferedReader(fileReader);

            while ((line = bufferedReader.readLine()) != null) {
                // If no longer required to create separate sub seed files, break
                if (index >= incrementalSizes.size()) {
                    break;
                }
                if (line.length() == 0) { // Empty line indicates division between sentences
                    if (sentenceStarted) {
                        stringBuilder.append(line).append("\n");
                        count++;
                        if (count == incrementalSizes.get(index)) { // Write the contents of the file so far to a new file
                            String fileName = this.executionDirectory + "/" + subFilePrefix + String.valueOf(count) + ".conllx";
                            writeToFile(fileName, stringBuilder);
                            index++;
                        }
                        sentenceStarted = false;
                    }
                } else { // If not an empty line, add to buffer
                    stringBuilder.append(line).append("\n");
                    sentenceStarted = true;
                }
            }
            if (index < incrementalSizes.size() && sentenceStarted) {
                stringBuilder.append("\n");
                count++;
                if (count == incrementalSizes.get(index)) { // Write the contents of the file so far to a new file
                    String fileName = this.executionDirectory + "/" + subFilePrefix + String.valueOf(count) + ".conllx";
                    writeToFile(fileName, stringBuilder);
                }
            }
            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int getNumberOfSentences(File file) {
        String line;

        boolean sentenceStarted = false;
        int count = 0;

        try {
            FileReader fileReader = new FileReader(file);
            BufferedReader bufferedReader = new BufferedReader(fileReader);

            while ((line = bufferedReader.readLine()) != null) {
                if (line.length() == 0) {
                    if (sentenceStarted) {
                        count++;
                        sentenceStarted = false;
                    }
                } else {
                    sentenceStarted = true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (sentenceStarted) {
            count++;
        }
        return count;
    }

    /*
     Do an initial parse over the brown conllx files genre wise and populate the appropriate 90% counts for each genre.
     */
    private void populateBrown90PercentCounts() {
        System.out.println(currentTime() + "90 Percent counts not given. Populating them by one-time pass through the files in the genres . . .");
        File baseDir = new File(brownCorpusBasePath);
        for (File genreDir: baseDir.listFiles()) {
            int count = 0;
            String name = genreDir.getName();
            System.out.println(currentTime() + "Parsing genre " + name + " . .");
            File[] files = genreDir.listFiles();
            if (files != null) {
                Arrays.sort(files); // To process the files in alphabetical order
            }
            for (File file: files) {
                count += getNumberOfSentences(file);
            }
            Integer trainingSetCount = (int) Math.round(count * 90.0 / 100.0);
            brown90PercentCounts.put(name, trainingSetCount);
        }
        System.out.println(currentTime() + "90 Percent counts populated: ");
        System.out.println(brown90PercentCounts);
        for (String key: brown90PercentCounts.keySet()) {
            System.out.println("brown90PercentCounts[" + key + "]: " + brown90PercentCounts.get(key));
        }
    }

    /*
     Create separate files for brown self-training set (90%) and brown test set (10%)
     */
    public void createBrownTrainingTestSplit() {
        if (brown90PercentCounts.size() == 0) {
            populateBrown90PercentCounts();
        }

        StringBuilder brownTrainingBuilder = new StringBuilder();
        StringBuilder brownTestBuilder = new StringBuilder();

        File baseDir = new File(brownCorpusBasePath);
        for (File genreDir: baseDir.listFiles()) {
            int count = 0;
            String name = genreDir.getName();
            int trainingCount = brown90PercentCounts.get(name);

            for (File file: genreDir.listFiles()) {
                boolean sentenceStarted = false;

                try {
                    String line;

                    FileReader fileReader = new FileReader(file);
                    BufferedReader bufferedReader = new BufferedReader(fileReader);

                    while ((line = bufferedReader.readLine()) != null) {
                        if (line.length() == 0) {
                            if (sentenceStarted) {
                                count++;
                                if (count <= trainingCount) {
                                    brownTrainingBuilder.append(line).append("\n");
                                } else {
                                    brownTestBuilder.append(line).append("\n");
                                }
                                sentenceStarted = false;
                            }
                        } else {
                            if (count <= trainingCount) {
                                brownTrainingBuilder.append(line).append("\n");
                            } else {
                                brownTestBuilder.append(line).append("\n");
                            }
                            sentenceStarted = true;
                        }
                    }
                    if (sentenceStarted) {
                        count++;
                        if (count <= trainingCount) {
                            brownTrainingBuilder.append("\n");
                        } else {
                            brownTestBuilder.append("\n");
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        String brownTrainingFileName = this.executionDirectory + "/" + "brown_training.conllx";
        String brownTestFileName = this.executionDirectory + "/" + "brown_test.conllx";

        writeToFile(brownTrainingFileName, brownTrainingBuilder);
        writeToFile(brownTestFileName, brownTestBuilder);

    }

    private void writeToFile(String fileName, StringBuilder stringBuilder) {
        System.out.println(currentTime() + "Writing to file " + fileName + " . . .");
        try {
            FileWriter fileWriter = new FileWriter(fileName);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write(stringBuilder.toString());
            bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void preprocess() {
        createWsjSeedFiles();
        copyWsjTrainingTestFilesIntoExecutionDir();

        createBrownTrainingTestSplit();
        createBrownTrainingFiles();
    }

    public static void main(String[] args) {
        String executionDirectory = args[0];
        String wsjSeedPath = args[1];
        String wsjTestPath = args[2];
        String brownCorpusBasePath = args[3];

        Preprocessor preprocessor = new Preprocessor(wsjSeedPath, wsjTestPath, brownCorpusBasePath, executionDirectory);

        preprocessor.createWsjSeedFiles();
        preprocessor.copyWsjTrainingTestFilesIntoExecutionDir();

        preprocessor.createBrownTrainingTestSplit();
        preprocessor.createBrownTrainingFiles();
    }
}
