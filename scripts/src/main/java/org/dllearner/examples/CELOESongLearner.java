package org.dllearner.examples;

import java.io.*;
import java.util.*;

import org.apache.commons.lang.ArrayUtils;
import org.apache.jena.base.Sys;
import org.dllearner.algorithms.celoe.CELOE;
import org.dllearner.core.ComponentInitException;
import org.dllearner.core.KnowledgeSource;
import org.dllearner.kb.OWLFile;
import org.dllearner.learningproblems.PosNegLP;
import org.dllearner.learningproblems.PosNegLPStandard;
import org.dllearner.learningproblems.PosOnlyLP;
import org.dllearner.reasoning.ClosedWorldReasoner;
import org.dllearner.refinementoperators.CustomStartRefinementOperator;
import org.dllearner.refinementoperators.ReasoningBasedRefinementOperator;
import org.dllearner.refinementoperators.RhoDRDown;
import org.dllearner.utilities.datastructures.SynchronizedSearchTree;
import org.openrdf.query.algebra.Str;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLIndividual;

import com.google.common.collect.Sets;

import uk.ac.manchester.cs.owl.owlapi.OWLClassExpressionImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLClassImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLNamedIndividualImpl;

/**
 * This class should exemplify how to run CELOE programmatically, i.e. by
 * creating all components explicitly in Java. This example runs the same
 * experiment as the examples/father.conf does.
 */
public class CELOESongLearner {
    static File familyExamplesDir = new File("./examples");
    Random random = new Random(System.currentTimeMillis());
    static String uriPrefix = "http://upb.de/Music#";
    protected String SongEmbeddingsFile = "C:/Users/cathe/IdeaProjects/DL-Learner/scripts/SpotifyLowHighEmbeddings.csv";
    protected String playlistEmbeddingsFile = "C:/Users/cathe/IdeaProjects/DL-Learner/scripts/SLHplaylistVector.csv";
    protected List<Double> defaultEmbedding;

//    protected String SongEmbeddingsFile = "/thesis/DL-Learner/scripts/SpotifyLowlevelEmbeddings.csv";
//    protected String playlistEmbeddingsFile = "/thesis/DL-Learner/scripts/SLplaylistVector.csv";


    protected HashMap<String, List<Double>> songVectorMap;
    protected HashMap<String, List<Double>> playlistVectorMap;
    protected Map<String, List<String>> playlistSongMap;
    protected List<String> existingSongs;
    protected List<String> trainingPlusTestSongs;

    CELOESongLearner() {
        this.songVectorMap = new HashMap<>();
        this.playlistVectorMap = new HashMap<>();
        this.playlistSongMap = new HashMap<>();
        this.existingSongs = new ArrayList<>();
        this.defaultEmbedding = new ArrayList<>();

    }

    /**
     * Create a playlist vector map
     */
    protected void createPlaylistVectorMap() {
        String line;
        List<Double> vectors;
        List<String> vectorFileRow;
        try {
            BufferedReader br = new BufferedReader(new FileReader(this.playlistEmbeddingsFile));
            while((line = br.readLine() ) != null) {
                vectorFileRow = Arrays.asList(line.split(","));
                if(!this.playlistVectorMap.containsKey(vectorFileRow.get(0))) {
                    vectors = new ArrayList<>();
                    for(int i=1; i<vectorFileRow.size(); i++) {
                        vectors.add(Double.parseDouble(vectorFileRow.get(i)));
                    }
                    this.playlistVectorMap.put(vectorFileRow.get(0), vectors);
                }
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Generate a map from song identifiers to their respective vectors from the embeddings file.
     *
     * @return
     */
    public void createSongVectorMap() {

        List<String> vectorFileRow;
        List<Double> vectors;
        String line;
        try {

            BufferedReader br = new BufferedReader(new FileReader(this.SongEmbeddingsFile));
            line = br.readLine();
            while((line = br.readLine()) != null) {
                vectorFileRow = Arrays.asList(line.split(","));
                if(!this.songVectorMap.containsKey(vectorFileRow.get(0))) {
                    vectors = new ArrayList<Double>();
                    for(int i=1; i<vectorFileRow.size(); i++) {
                        vectors.add(Double.parseDouble(vectorFileRow.get(i)));
                    }
                    this.songVectorMap.put("Music#" + vectorFileRow.get(0), vectors);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * Create a map of the individuals satisfying a class expression and their cosine similarity to
     * the centroid vector of the given playlist
     * @param playlist
     * @return return the map
     */
    protected Map<String, Double> createIndividualSimilarityMap (String playlist) {
        String individual;
        Map<String, Double> individualSimilarity = new HashMap<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader("C:/Users/cathe/IdeaProjects/DL-Learner/scripts/classIndividuals.txt"));
//            BufferedReader br = new BufferedReader(new FileReader("/thesis/DL-Learner/scripts/classIndividuals.txt"));
            while((individual = br.readLine()) != null) {
                //System.out.println("playlist: " + playlist + "individual: " + individual );
                if(this.songVectorMap.get(individual) == null) {
                    for(int i=0; i<200; i++) {
                        this.defaultEmbedding.add(-0.018702794);
                    }
                    this.songVectorMap.put(individual, this.defaultEmbedding);
                }
                //System.out.println(playlist + ": " + this.playlistVectorMap.get(playlist) + "\n" + individual + ": " + this.songVectorMap.get(individual) );
                double cosineSimilarity = cosineSimilarity(this.playlistVectorMap.get(playlist), this.songVectorMap.get(individual));
                individualSimilarity.put(individual, cosineSimilarity);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return individualSimilarity;
    }


    public void  writeResults(String playlist, List<String> trainSongs, List<String> testSongs) {
        Map<String, Double> individualSimilarityMap = createIndividualSimilarityMap(playlist);
        for(String song: trainSongs) {
            individualSimilarityMap.remove("Music#" + song);
        }

        LinkedHashMap<String, Double> sortedMap = new LinkedHashMap<>();
        individualSimilarityMap.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .forEachOrdered(x -> sortedMap.put(x.getKey(), x.getValue()));
        List<String> sortedIndividuals = new ArrayList<>();
        for(String key: sortedMap.keySet()) {
            sortedIndividuals.add(key);
        }

        try {
            FileWriter writer = new FileWriter("C:/Users/cathe/IdeaProjects/DL-Learner/scripts/resultsSLH.txt", true);
//            FileWriter writer = new FileWriter("/thesis/DL-Learner/scripts/resultsSpotifyLowlevel.txt", true);
            writer.write(playlist);
            writer.write("\n");
            for(String song: testSongs) {
                writer.write(song + ": " + sortedIndividuals.indexOf(song));
                writer.write("\n");
            }
            writer.flush();
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    /**
     * Find cosine similarity between 2 vectors.
     * @param vectorA
     * @param vectorB
     * @return Double value indicating the cosine similarity
     */
    public Double cosineSimilarity(List<Double> vectorA, List<Double> vectorB) {
        Double dotProduct = 0.0;
        Double normA = 0.0;
        Double normB = 0.0;
        for (int i = 0; i < vectorA.size(); i++) {
            dotProduct += vectorA.get(i) * vectorB.get(i);
            normA += Math.pow(vectorA.get(i), 2);
            normB += Math.pow(vectorB.get(i), 2);
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    protected void createPlaylistSongsMap() {
        try {
            BufferedReader csvReader = new BufferedReader(new FileReader("C:/Users/cathe/IdeaProjects/DL-Learner/scripts/playlistSongs.csv"));
//            BufferedReader csvReader = new BufferedReader(new FileReader("/thesis/DL-Learner/scripts/playlistSongs.csv"));
            String row;
            while ((row = csvReader.readLine())!= null) {
                String[] rowSplit = row.split(",");
                this.playlistSongMap.putIfAbsent(rowSplit[3], new ArrayList<>());
                this.playlistSongMap.get(rowSplit[3]).add(rowSplit[4]);

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    protected void retrieveExistingSongs() {
        try {
            BufferedReader csvReader = new BufferedReader(new FileReader("C:/Users/cathe/IdeaProjects/DL-Learner/scripts/SpotifyLowHighData.csv"));
//            BufferedReader csvReader = new BufferedReader(new FileReader("/thesis/DL-Learner/scripts/SpotifyLowlevelData.csv"));

            String row = csvReader.readLine();
            while((row = csvReader.readLine())!= null) {
                String[] rowSplit = row.split(",");
                this.existingSongs.add(rowSplit[18]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieve the positive examples for a playlist
     * @param playlist
     * @return
     */
    protected HashSet<OWLIndividual> getPositiveExamples(String playlist) {
        this.trainingPlusTestSongs = new ArrayList<>();
        HashSet<OWLIndividual> positiveExamples = new HashSet<>();
        for(String song: this.playlistSongMap.get(playlist)) {
            if(this.existingSongs.contains(song)) {
                positiveExamples.add(new OWLNamedIndividualImpl(IRI.create(uriPrefix + song)));
                this.trainingPlusTestSongs.add("Music#" + song);
            }
        }
        //System.out.println(this.trainingPlusTestSongs);
        return positiveExamples;
    }

    /**
     * Sample the negative examples to retrieve 1000 negative examples.
     * @param playlist
     * @return
     */
    protected HashSet<OWLIndividual> getNegativeExamples(String playlist) {
        HashSet<OWLIndividual> negativeExamples = new HashSet<>();
        for(String song: this.existingSongs) {
            if(!this.playlistSongMap.get(playlist).contains(song)) {
                negativeExamples.add(new OWLNamedIndividualImpl(IRI.create(uriPrefix + song)));
            }
        }
        HashSet<OWLIndividual> sampledNegExamples = new HashSet<>();
        int i=0;
        for(OWLIndividual song: negativeExamples) {
            if(i>1000)
                break;
            if(!sampledNegExamples.contains(song)) {
                sampledNegExamples.add(song);
                i++;
            }
        }
        return sampledNegExamples;
    }




    public static void main(String[] args) throws ComponentInitException {
        CELOESongLearner songLearner = new CELOESongLearner();
        Random random = new Random(System.currentTimeMillis());
        OWLFile ks = new OWLFile();
        //ks.setFileName(familyExamplesDir.getAbsolutePath() + "/music.owl");
        ks.setFileName("C:/Users/cathe/IdeaProjects/DL-Learner/scripts/SpotifyLowHighKG.owl");
//        ks.setFileName("/thesis/DL-Learner/scripts/SpotifyLowHighKG.owl");
        ks.init();

        ClosedWorldReasoner reasoner = new ClosedWorldReasoner();
        Set<KnowledgeSource> sources = new HashSet<>();
        sources.add(ks);
        reasoner.setSources(sources);
        reasoner.init();
        PosNegLPStandard lp = new PosNegLPStandard(reasoner);


        HashSet<OWLIndividual> playlistSongs = new HashSet<>();
        List<String> trainSongs = new ArrayList<>();
        List<String> testSongs = new ArrayList<>();
        HashSet<OWLIndividual> sampledNegExamples = new HashSet<>();
        BufferedReader csvReader = null;
        Map<String, List<OWLIndividual>> propertyMap = new HashMap<>();
        //String playlist = "NTC_ Gym Strong";
        String[] playlists = {"Rock Me UP!", "One More Rep", "Songs to Sing in the Car", "Alternative 90s", "90s Pop Rock Essentials", "Nike Running Tempo Mix", "Autumn Leaves", "Wake Up Happy"};
        songLearner.createPlaylistSongsMap();
        songLearner.retrieveExistingSongs();
        songLearner.createSongVectorMap();
        songLearner.createPlaylistVectorMap();

        try {
            //csvReader = new BufferedReader(new FileReader(familyExamplesDir.getAbsolutePath() + "/preprocessedSongData2.csv"));
            csvReader = new BufferedReader(new FileReader("C:/Users/cathe/IdeaProjects/DL-Learner/scripts/SpotifyLowHighData.csv"));
//            csvReader = new BufferedReader(new FileReader("/thesis/DL-Learner/scripts/SpotifyLowHighData.csv"));
            String row = csvReader.readLine();
            String[] songProperties = row.split(",");
            for(String property: songProperties) {
                propertyMap.putIfAbsent(property, new ArrayList<>());
            }
            while((row = csvReader.readLine()) != null) {
                String[] rowElements = row.split(",");
                for(String property: songProperties) {
                    int index = ArrayUtils.indexOf(songProperties, property);
                    if(! propertyMap.get(property).contains(uriPrefix + property + "_" + rowElements[index]) && !rowElements[index].equals("NA")){
                        propertyMap.get(property).add(new OWLNamedIndividualImpl(IRI.create(uriPrefix + property + "_" + rowElements[index])));
                    }
                }
//
            }
            csvReader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        for(String playlist: playlists) {

            try {
                FileWriter measureWriter = new FileWriter("C:/Users/cathe/IdeaProjects/DL-Learner/scripts/SLHAccuracy.txt", true);
                measureWriter.append("\n" + playlist + ": ");
                measureWriter.flush();
                measureWriter.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            playlistSongs = songLearner.getPositiveExamples(playlist);
            // splitting train songs and test songs
            int count = 0;
            testSongs = new ArrayList<>();
            trainSongs = new ArrayList<>();
            for(int i=songLearner.trainingPlusTestSongs.size()-1; i>=0; i--) {
                if(count < 10) {
                    testSongs.add(songLearner.trainingPlusTestSongs.get(i));
                }
                else {
                    trainSongs.add(songLearner.trainingPlusTestSongs.get(i));
                }
                count ++;
            }
            // sampling songs from the dataset for negative examples
            sampledNegExamples = songLearner.getNegativeExamples(playlist);
            List nonProperties = new ArrayList();
            nonProperties.add("Playlist");
            nonProperties.add("Speechiness");
            nonProperties.add("Song");
            nonProperties.add("song_name");
//            nonProperties.add("Artist");
            nonProperties.add("MBID");
            nonProperties.add("Acoustic");
            nonProperties.add("spectral_decrease");

            // Adding all the idividuals other than songs to negative examples list
            for(String key: propertyMap.keySet()) {
                if(!nonProperties.contains(key)) {
                    for(OWLIndividual value: propertyMap.get(key)) {
                        sampledNegExamples.add(value);
                    }
                }
            }
            lp.setPositiveExamples(playlistSongs);
            lp.setNegativeExamples(sampledNegExamples);
            lp.init();
            CELOE alg = new CELOE(lp, reasoner);
            alg.setMaxExecutionTimeInSeconds(10);
            alg.setStartClass(new OWLClassImpl(IRI.create(uriPrefix + "Song")));
            RhoDRDown op = new RhoDRDown();
            op.setUseHasValueConstructor(true);
            op.setUseDataHasValueConstructor(true);
            op.setStartClass(new OWLClassImpl(IRI.create(uriPrefix + "Song")));
            op.setReasoner(reasoner);
            op.init();
            alg.setOperator(op);
            alg.setLearningProblem(lp);
            alg.setReasoner(reasoner);
            alg.init();
            alg.start();
            songLearner.writeResults(playlist, trainSongs, testSongs);
        }

    }
}
