package edu.utexas.nlp.hw3;

import edu.stanford.nlp.parser.nndep.DependencyParser;

import java.io.*;
import java.util.*;

/**
 * Created by pandian on 3/25/17.
 */
public class DependencyParserDriver {
    private String execDir;
    private String seedSet;
    private String testSet;
    private String experimentType;
    private String embeddingPath;
    private int seedSizeSingle;
    private int selfTrainingSizeSingle;
    private boolean varySeedSize;
    private boolean varySelfTrainingSize;

    private List<Integer> wsjSeedSetSizes;
    private List<Integer> brownSelfTrainingSizes;

    private String currentTime() {
        return new Date().toString() + " ";
    }

    public DependencyParserDriver(String[] args) {
        parseCommandLineArgs(args);

        wsjSeedSetSizes = new ArrayList<>(Arrays.asList(1000, 2000, 3000, 4000, 5000, 7000, 10000, 13000, 16000, 20000, 25000, 30000, 35000));
        brownSelfTrainingSizes = new ArrayList<>(Arrays.asList(1000, 2000, 3000, 4000, 5000, 7000, 10000, 13000, 17000, 21000));
    }

    private void printUsage() {
        System.out.println("java edu.utexas.nlp.hw3.DependencyParserDriver <execution_dir> <seed_corpus - wsj/brown> <test_corpus - wsj/brown> <embedding_file_path> <single> <seed_set_size> [<self_training_size>]");
        System.out.println("java edu.utexas.nlp.hw3.DependencyParserDriver <execution_dir> <seed_corpus - wsj/brown> <test_corpus - wsj/brown> <embedding_file_path> <group> <vary_seed_size/vary_self_training_size>");
    }

    private void validateSeedTestCorpus(String corpus) {
        if (!corpus.equalsIgnoreCase("wsj") && !corpus.equalsIgnoreCase("brown")) {
            System.out.println("Invalid corpus name " + corpus + " given for seed or test set. Please provide wsj/brown. ");
        }
    }

    private void validateExperimentType(String type) {
        if (!type.equalsIgnoreCase("single") && !type.equalsIgnoreCase("group")) {
            System.out.println("Invalid experiment type " + type + " provided. Please provide single/group. ");
        }
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

    private void concatenateFiles(String source1, String source2, String destination) {
        StringBuilder stringBuilder = new StringBuilder();
        readFromFile(source1, stringBuilder);
        readFromFile(source2, stringBuilder);
        writeToFile(destination, stringBuilder);
    }

    private void process() {
        String trainPath, testPath, modelPath, testAnnotationsPath, selfTrainingPath, newTrainPath;
        if (experimentType.equalsIgnoreCase("single")) {
            if (this.selfTrainingSizeSingle != 0) { // adaptation and retraining
                testPath = execDir + "/" + testSet + "_test.conllx";
                if (seedSizeSingle == -1) {
                    trainPath = execDir + "/" + seedSet + "_training.conllx";
                } else {
                    trainPath = execDir + "/" + seedSet + "_seed_self_training_" + String.valueOf(seedSizeSingle) + ".conllx";
                }
                if (selfTrainingSizeSingle == -1) {
                    selfTrainingPath = execDir + "/" + testSet + "_training.conllx";
                } else {
                    selfTrainingPath = execDir + "/" + testSet + "_seed_self_training_" + String.valueOf(selfTrainingSizeSingle) + ".conllx";
                }
                modelPath = execDir + "/" + "model_" + seedSet + "_" + testSet + "_adapt";
                testAnnotationsPath = execDir + "/" + "self_train_set_annotations" + seedSet + "_" + testSet + ".conllx";
                annotateTestSet(trainPath, selfTrainingPath, modelPath, testAnnotationsPath);

                // Combine train file and new self_train_set_annotations into a new train file
                newTrainPath = execDir + "/" + "combined_" + seedSet + "_" + testSet + "_seed_size_" + seedSizeSingle + ".conllx";
                concatenateFiles(trainPath, testAnnotationsPath, newTrainPath);

                modelPath = execDir + "/" + "model_" + seedSet + "_" + testSet + "_adapt_combined";
                testAnnotationsPath = execDir + "/" + "test_set_annotations" + seedSet + "_" + testSet + ".conllx";
                annotateTestSet(newTrainPath, testPath, modelPath, testAnnotationsPath);
            } else {
                testPath = execDir + "/" + testSet + "_test.conllx";
                if (seedSizeSingle == -1) {
                    trainPath = execDir + "/" + seedSet + "_training.conllx";
                } else {
                    trainPath = execDir + "/" + seedSet + "_seed_self_training_" + String.valueOf(seedSizeSingle) + ".conllx";
                }
                modelPath = execDir + "/" + "model_" + seedSet + "_" + testSet;
                testAnnotationsPath = execDir + "/" + "test_set_annotations_" + seedSet + "_" + testSet + ".conllx";
                annotateTestSet(trainPath, testPath, modelPath, testAnnotationsPath);
            }
        } else {
            if (varySeedSize) {
                List<Integer> incrementalSeedSizes;
                if (seedSet.equalsIgnoreCase("wsj")) {
                    incrementalSeedSizes = wsjSeedSetSizes;
                } else {
                    incrementalSeedSizes = brownSelfTrainingSizes;
                }

                for (int seedSize: incrementalSeedSizes) {
                    // Train on seedSet and test on seedSet
                    trainPath = execDir + "/" + seedSet + "_seed_self_training_" + String.valueOf(seedSize) + ".conllx";
                    testPath = execDir + "/" + seedSet + "_test.conllx";
                    modelPath = execDir + "/" + "model_seed_size_" + String.valueOf(seedSize) + "_" + seedSet + "_" + seedSet;
                    testAnnotationsPath = execDir + "/" + "test_annotations_seed_size_" + String.valueOf(seedSize) + "_" + seedSet + "_" + seedSet + ".conllx";
                    annotateTestSet(trainPath, testPath, modelPath, testAnnotationsPath);

                    // Train on seedSet and test on testSet
                    trainPath = execDir + "/" + seedSet + "_seed_self_training_" + String.valueOf(seedSize) + ".conllx";
                    testPath = execDir + "/" + testSet + "_test.conllx";
                    modelPath = execDir + "/" + "model_seed_size_" + String.valueOf(seedSize) + "_" + seedSet + "_" + testSet;
                    testAnnotationsPath = execDir + "/" + "test_annotations_seed_size_" + String.valueOf(seedSize) + "_" + seedSet + "_" + testSet + ".conllx";
                    annotateTestSet(trainPath, testPath, modelPath, testAnnotationsPath);

                    // Train on seedSet, adapt on testSet and test on testSet
                    trainPath = execDir + "/" + seedSet + "_seed_self_training_" + String.valueOf(seedSize) + ".conllx";
                    selfTrainingPath = execDir + "/" + testSet + "_training.conllx";
                    modelPath = execDir + "/" + "model_seed_size_" + String.valueOf(seedSize) + "_" + seedSet + "_" + testSet + "_" + "adapt";
                    testAnnotationsPath = execDir + "/" + "self_train_set_annotations_seed_size_" + String.valueOf(seedSize) + "_" + seedSet + "_" + testSet + ".conllx";
                    annotateTestSet(trainPath, selfTrainingPath, modelPath, testAnnotationsPath);

                    // Combine train file and new self_train_set_annotations into a new train file
                    newTrainPath = execDir + "/" + "combined_" + seedSet + "_" + testSet + "_seed_size_" + seedSize + ".conllx";
                    concatenateFiles(trainPath, testAnnotationsPath, newTrainPath);

                    testPath = execDir + "/" + testSet + "_test.conllx";
                    modelPath = execDir + "/" + "model_seed_size_" + String.valueOf(seedSize) + "_" + seedSet + "_" + testSet + "_adapt_combined";
                    testAnnotationsPath = execDir + "/" + "test_set_annotations_seed_size_" + String.valueOf(seedSize) + "_" + seedSet + "_" + testSet + ".conllx";
                    annotateTestSet(newTrainPath, testPath, modelPath, testAnnotationsPath);
                }
            } else {
                List<Integer> incrementalSelfTrainingSizes;
                if (seedSet.equalsIgnoreCase("wsj")) {
                    incrementalSelfTrainingSizes = wsjSeedSetSizes;
                } else {
                    incrementalSelfTrainingSizes = brownSelfTrainingSizes;
                }
                int seedSize = 10000;
                for (int selfTrainingSize: incrementalSelfTrainingSizes) {
                    // Train on seedSet, adapt on testSet and test on testSet
                    trainPath = execDir + "/" + seedSet + "_seed_self_training_" + String.valueOf(seedSize) + ".conllx";
                    selfTrainingPath = execDir + "/" + testSet + "_seed_self_training_" + String.valueOf(selfTrainingSizeSingle) + ".conllx";

                    modelPath = execDir + "/" + "model_self_training_size_" + String.valueOf(selfTrainingSize) + "_" + seedSet + "_" + testSet + "_" + "adapt";
                    testAnnotationsPath = execDir + "/" + "self_train_set_annotations_self_training_size_" + String.valueOf(selfTrainingSize) + "_" + seedSet + "_" + testSet + ".conllx";
                    annotateTestSet(trainPath, selfTrainingPath, modelPath, testAnnotationsPath);

                    // Combine train file and new self_train_set_annotations into a new train file
                    newTrainPath = execDir + "/" + "combined_" + seedSet + "_" + testSet + "_self_training_size_" + selfTrainingSize + ".conllx";
                    concatenateFiles(trainPath, testAnnotationsPath, newTrainPath);

                    testPath = execDir + "/" + testSet + "_test.conllx";
                    modelPath = execDir + "/" + "model_self_training_size_" + String.valueOf(selfTrainingSize) + "_" + seedSet + "_" + testSet + "_adapt_combined";
                    testAnnotationsPath = execDir + "/" + "test_set_annotations_self_training_size_" + String.valueOf(selfTrainingSize) + "_" + seedSet + "_" + testSet + ".conllx";
                    annotateTestSet(newTrainPath, testPath, modelPath, testAnnotationsPath);
                }
            }
        }
    }

    private void parseCommandLineArgs(String[] args) {
        if (args.length < 6) {
            printUsage();
        }
        this.execDir = args[0];
        this.seedSet = args[1];
        this.testSet = args[2];
        this.embeddingPath = args[3];
        this.experimentType = args[4];

        validateSeedTestCorpus(this.seedSet);
        validateSeedTestCorpus(this.testSet);
        validateExperimentType(this.experimentType);

        if (experimentType.equalsIgnoreCase("single")) {
            if (args[5].equalsIgnoreCase("full")) {
                seedSizeSingle = -1;
            } else {
                seedSizeSingle = Integer.parseInt(args[5]);
            }
            if (args.length >= 7) {
                if (args[6].equalsIgnoreCase("full")) {
                    selfTrainingSizeSingle = -1;
                } else {
                    selfTrainingSizeSingle = Integer.parseInt(args[6]);
                }
            } else {
                selfTrainingSizeSingle = 0; // It means no adaptation and so retraining
            }
        } else {
            if (args[5].equalsIgnoreCase("vary_seed_set_size")) {
                this.varySeedSize = true;
                this.varySelfTrainingSize = false;
            } else if (args[5].equalsIgnoreCase("vary_self_training_size")) {
                this.varySeedSize = false;
                this.varySelfTrainingSize = true;
            } else {
                System.out.println("Invalid vary method for group " + args[5] + " given. Please provide vary_seed_set_size or vary_self_training_size. ");
            }
        }
    }

    private void annotateTestSet(String trainPath, String testPath, String modelPath, String testAnnotationsPath) {
        // Configuring propreties for the parser. A full list of properties can be found
        // here https://nlp.stanford.edu/software/nndep.shtml
        Properties prop = new Properties();
        prop.setProperty("maxIter", "20");

        DependencyParser p = new DependencyParser(prop);

        // Argument 1 - Training Path
        // Argument 2 - Dev Path (can be null)
        // Argument 3 - Path where model is saved
        // Argument 4 - Path to embedding vectors (can be null)
        p.train(trainPath, null, modelPath, embeddingPath);

        // Load a saved path
        DependencyParser model = DependencyParser.loadFromModelFile(modelPath);

        // Test model on test data, write annotations to testAnnotationsPath
        model.testCoNLL(testPath, testAnnotationsPath);
    }

    public static void main(String[] args) {
        DependencyParserDriver driver = new DependencyParserDriver(args);
        driver.process();
    }
}
