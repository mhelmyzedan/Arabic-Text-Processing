/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package arabictextpreprocessing;

import edu.columbia.ccls.madamira.MADAMIRAWrapper;
import edu.columbia.ccls.madamira.configuration.MadamiraInput;
import edu.columbia.ccls.madamira.configuration.MadamiraOutput;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.util.concurrent.ExecutionException;
import arabictextprocessing.ArabicDocProcessing;
import edu.columbia.ccls.madamira.configuration.Chunk;
import edu.columbia.ccls.madamira.configuration.OutSeg;
import edu.columbia.ccls.madamira.configuration.Tok;
import edu.columbia.ccls.madamira.configuration.Word;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
/**
 *
 * @author Muhammad Helmy
 * Based on APIExampleUse.java from MADAMIRA distribution
 */

//MADAMIRA transforms the Farsi characters into their equivalents in Arabic. For ex:  گ، پ، ژ، چ to ك,ب,ز,ج

public class MADAMIRA_preProcessing {
    // MADAMIRA namespace as defined by its XML schema
    private static final String MADAMIRA_NS = "edu.columbia.ccls.madamira.configuration";
    private static final String INPUT_FILE = "/mnt/HDD/AraWordEmbedding/test/SampleInputFile.xml";
    private static final String OUTPUT_FILE = "/mnt/HDD/AraWordEmbedding/test/sampleOutputFile.xml";
    static BufferedReader reader;
    static String configStr;
    static MadamiraInput input;
    static MadamiraOutput output;
    static MADAMIRAWrapper wrapper;
    static Unmarshaller unmarshaller;
    static JSONParser parser;
    static PrintStream segmentedFile, lemmatizedFile, stemmedFile, errorsFile;
    static String suffix;
    public static void main(String [] args) throws Exception{
        MADAMIRA_preProcessing mp = new MADAMIRA_preProcessing("ArabicWeb01");
        mp.preprocessFiles("/mnt/HDD/arWE/resources/ArabicWeb16/ProcessedFiles");
        //preprocessSingleFile("/mnt/HDD/arWE/resources/Wikipedia/wikipediaProcessedTxt.txt3", "Wikipedia", false);
        //preprocessSingleFile("/mnt/HDD/arWE/resources/ArabicWeb16/processed01.json", "ArabicWeb16", true);
        //preprocess();
        //testStanfordSegmenter();
    } 
    public MADAMIRA_preProcessing(String suffix) throws Exception{
        this.suffix = suffix;
        ClassLoader classLoader = MADAMIRA_preProcessing.class.getClassLoader();
        InputStream is = classLoader.getResourceAsStream("configFile.txt");
        reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        configStr = "";
        for (String line; (line = reader.readLine()) != null;) 
            configStr += line;
        is.close();
        reader.close();
        segmentedFile = new PrintStream( new FileOutputStream("/mnt/HDD/arWE/resources/ProcessedFiles/segmented_" + suffix + ".txt", true), true, "UTF-8");
        lemmatizedFile = new PrintStream( new FileOutputStream("/mnt/HDD/arWE/resources/ProcessedFiles/lemmatized_" + suffix + ".txt", true), true, "UTF-8");
        stemmedFile = new PrintStream( new FileOutputStream("/mnt/HDD/arWE/resources/ProcessedFiles/stemmed_" + suffix + ".txt", true), true, "UTF-8");
        errorsFile = new PrintStream( new FileOutputStream("/mnt/HDD/arWE/resources/ProcessedFiles/errors_" + suffix + ".txt", true), true, "UTF-8");

        wrapper = new MADAMIRAWrapper();
        JAXBContext jc = JAXBContext.newInstance(MADAMIRA_NS);
        unmarshaller = jc.createUnmarshaller();            
        parser = new JSONParser();        
    }
    public static void preprocessSingleFile(String filePath, boolean isJSON) throws Exception{
        //Normalization =====> Take care
        int count = 0;
        String line = "";
        //String filePath = "/mnt/HDD/arWE/resources/Wikipedia/wikipediaProcessedTxt.txt3";
        reader = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8));            
        for (; (line = reader.readLine()) != null;){
            count++;
            /*if(count < 6037)
                continue;*/
            try {
                if(isJSON){
                    JSONObject jsonObject = ((JSONObject)parser.parse(line.trim()));
                    line = ((String)jsonObject.get("title")) + " . " +((String)jsonObject.get("text"));//.replaceAll("[&<>]", " ");
                    /*System.out.println(line);
                    return;*/
                }

                String txt = configStr + line.trim() + "</in_seg></in_doc></madamira_input>";
                input = (MadamiraInput)unmarshaller.unmarshal(new ByteArrayInputStream(txt.getBytes(StandardCharsets.UTF_8)));
                output = wrapper.processString(input);
                List<OutSeg> outSegs = output.getOutDoc().getOutSeg();
                StringBuffer segmentedText = new StringBuffer();
                StringBuffer lemmatizedText = new StringBuffer();
                StringBuffer stemmedText = new StringBuffer();
                for (OutSeg outSeg : outSegs) {
                    List<Chunk> chunks = outSeg.getSegmentInfo().getBpc().getChunk();                    
                    for (Chunk chunk : chunks) {
                        List<Tok> toks = chunk.getTok();
                        for (Tok tok : toks) {
                            segmentedText.append(tok.getForm0() + " ");                            
                        }                        
                    }
                    List<Word> words = outSeg.getWordInfo().getWord();
                    for(Word word : words){
                        //System.out.println(word.getWord());
                        if(word.getAnalysis().size() > 0){
                            lemmatizedText.append(word.getAnalysis().get(0).getMorphFeatureSet().getLemma() + " ");
                            stemmedText.append(word.getAnalysis().get(0).getMorphFeatureSet().getStem()+ " ");
                        }
                        else{
                            lemmatizedText.append(word.getWord() + " ");
                            stemmedText.append(word.getWord() + " ");
                        }
                    }
                }
                //out.println(ArabicDocProcessing.removeNonArabicText(segmentedText.toString().trim()));
                segmentedFile.println(segmentedText.toString().trim().replaceAll("[^\u0621-\u063A\u0641-\u064A ]+", "").replaceAll("[ ]{2,}", " "));
                lemmatizedFile.println(lemmatizedText.toString().trim().replaceAll("[^\u0621-\u063A\u0641-\u064A ]+", "").replaceAll("[ ]{2,}", " "));
                stemmedFile.println(stemmedText.toString().trim().replaceAll("[^\u0621-\u063A\u0641-\u064A ]+", "").replaceAll("[ ]{2,}", " "));                
                /*if(count>3)
                    break;*/
                System.out.println(count + " : " + filePath);
            } catch(Exception e){
                System.out.println(count + " : Exception");
                e.printStackTrace();
                errorsFile.println(line.trim());
            }
        }
    }
    public static void preprocessFiles(String dirPath){
        //Normalization =====> Take care
        try{
            Path start = Paths.get(dirPath);
            List<String> collect;
            try (Stream<Path> stream = Files.walk(start, Integer.MAX_VALUE)) {
                collect = stream.filter(Files::isRegularFile)
                    .map(String::valueOf)
                    .sorted()
                    .collect(Collectors.toList());
                //collect.forEach(System.out::println);
            }
            for(String filePath : collect){
                if(filePath.startsWith(dirPath + "/processed"))
                    preprocessSingleFile(filePath, true);
            }            
        } catch(Exception e){
            e.printStackTrace();
        }
        segmentedFile.close();
        lemmatizedFile.close();
        stemmedFile.close();
        errorsFile.close();
        wrapper.shutdown();
    }
    
    public static void preprocess() throws Exception{
        //Normalization =====> Take care
        ClassLoader classLoader = MADAMIRA_preProcessing.class.getClassLoader();
	InputStream is = classLoader.getResourceAsStream("configFile.txt");
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        String configStr = "";
        for (String line; (line = reader.readLine()) != null;) 
            configStr += line;
        is.close();
        reader.close();
        Path start = Paths.get("/mnt/HDD/AraWordEmbedding/resources/Wikipedia/WikipediaPages_JSON");
        String finalFilePath = "/mnt/HDD/AraWordEmbedding/test/originalFile.txt";
        PrintStream segmentedFile = new PrintStream( new FileOutputStream("/mnt/HDD/AraWordEmbedding/resources/Wikipedia/segmentedFile.txt", true), true, "UTF-8");
        PrintStream lemmatizedFile = new PrintStream( new FileOutputStream("/mnt/HDD/AraWordEmbedding/resources/Wikipedia/lemmatizedFile.txt", true), true, "UTF-8");
        PrintStream stemmedFile = new PrintStream( new FileOutputStream("/mnt/HDD/AraWordEmbedding/resources/Wikipedia/stemmedFile.txt", true), true, "UTF-8");
        PrintStream errorsFile = new PrintStream( new FileOutputStream("/mnt/HDD/arWE/resources/ProcessedFiles/errors_Wikipedia.txt", true), true, "UTF-8");
        List<String> collect;
        try (Stream<Path> stream = Files.walk(start, Integer.MAX_VALUE)) {
	    collect = stream.filter(Files::isRegularFile)
	        .map(String::valueOf)
	        .sorted()
	        .collect(Collectors.toList());
	    //collect.forEach(System.out::println);
	}
        
        final MADAMIRAWrapper wrapper = new MADAMIRAWrapper();
        JAXBContext jc = JAXBContext.newInstance(MADAMIRA_NS);
        Unmarshaller unmarshaller = jc.createUnmarshaller();
        MadamiraInput input;
        MadamiraOutput output;
        JSONParser parser = new JSONParser();
        int count = 0;
        String line = "";
        try {
            for(String filePath : collect){
                // The structure of the MadamiraInput object is exactly similar to the
                // madamira_input element in the XML
                
                //String filePath1 = "/mnt/HDD/AraWordEmbedding/test/SampleInputFile.xml";
                reader = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8));
                for (; (line = reader.readLine()) != null;){
                    line = ((String)((JSONObject)parser.parse(line)).get("text")).replaceAll("[&<>]", " ");
                    /*if(line.contains("ماوراءالنهر")){
                        System.out.println(filePath);
                        System.exit(0);                        
                    }
                    else continue;*/
                    //line.matches("")
                    String txt = configStr + line + "</in_seg></in_doc></madamira_input>";
                    input = (MadamiraInput)unmarshaller.unmarshal(new ByteArrayInputStream(txt.getBytes(StandardCharsets.UTF_8)));

                    /*{
                        int numSents = input.getInDoc().getInSeg().size();
                        String outputAnalysis = input.getMadamiraConfiguration().
                                getOverallVars().getOutputAnalyses();
                        String outputEncoding = input.getMadamiraConfiguration().
                                getOverallVars().getOutputEncoding();

                        System.out.println("processing " + numSents +
                                " sentences for analysis type = " + outputAnalysis +
                                " and output encoding = " + outputEncoding);
                    }*/

                    // The structure of the MadamiraOutput object is exactly similar to the
                    // madamira_output element in the XML
                    output = wrapper.processString(input);
                    List<OutSeg> outSegs = output.getOutDoc().getOutSeg();
                    StringBuffer segmentedText = new StringBuffer();
                    StringBuffer lemmatizedText = new StringBuffer();
                    StringBuffer stemmedText = new StringBuffer();
                    for (OutSeg outSeg : outSegs) {
                        List<Chunk> chunks = outSeg.getSegmentInfo().getBpc().getChunk();                    
                        for (Chunk chunk : chunks) {
                            List<Tok> toks = chunk.getTok();
                            for (Tok tok : toks) {
                                segmentedText.append(tok.getForm0() + " ");                            
                            }                        
                        }
                        List<Word> words = outSeg.getWordInfo().getWord();
                        for(Word word : words){
                            //System.out.println(word.getWord());
                            if(word.getAnalysis().size() > 0){
                                lemmatizedText.append(word.getAnalysis().get(0).getMorphFeatureSet().getLemma() + " ");
                                stemmedText.append(word.getAnalysis().get(0).getMorphFeatureSet().getStem()+ " ");
                            }
                            else{
                                lemmatizedText.append(word.getWord() + " ");
                                stemmedText.append(word.getWord() + " ");
                            }
                        }

                    }
                    //out.println(ArabicDocProcessing.removeNonArabicText(segmentedText.toString().trim()));
                    segmentedFile.println(segmentedText.toString().trim().replaceAll("[^\u0621-\u063A\u0641-\u064A ]+", "").replaceAll("[ ]{2,}", " "));
                    lemmatizedFile.println(lemmatizedText.toString().trim().replaceAll("[^\u0621-\u063A\u0641-\u064A ]+", "").replaceAll("[ ]{2,}", " "));
                    stemmedFile.println(stemmedText.toString().trim().replaceAll("[^\u0621-\u063A\u0641-\u064A ]+", "").replaceAll("[ ]{2,}", " "));
                    /*{
                        int numSents = output.getOutDoc().getOutSeg().size();

                        System.out.println("processed output contains "+numSents+" sentences...");
                    }*/


                    //jc.createMarshaller().marshal(output, new File(OUTPUT_FILE));
                    /*count++;
                    if(count>3)
                        break;*/
                    System.out.println(count++ + " : " + filePath);
                }
                //break;
                System.out.println(count++ + " : " + filePath);
            }
            segmentedFile.close();
            lemmatizedFile.close();
            stemmedFile.close();
            
        } catch (JAXBException ex) {
            System.out.println("Error marshalling or unmarshalling data: ");
                    ex.printStackTrace();
        } catch (InterruptedException ex) {
            System.out.println("MADAMIRA thread interrupted: "
                    +ex.getMessage());
        } catch (ExecutionException ex) {
            System.out.println("Unable to retrieve result of task. " +
                    "MADAMIRA task may have been aborted: "+ex.getCause());
        } catch(Exception e){
            System.out.println(count + " : Exception");
            e.printStackTrace();
            errorsFile.println(line.trim());
        }

        wrapper.shutdown();
    }
    private static void testStanfordSegmenter(){
        ArabicDocProcessing.init();
        ArabicDocProcessing.initialize("/mnt/HDD/AraWordEmbedding/Tools/resources");
        String text = ArabicDocProcessing.readDocumentText("/mnt/HDD/AraWordEmbedding/test/test.txt");
        System.out.println(ArabicDocProcessing.segmentText(ArabicDocProcessing.purifyDoc(ArabicDocProcessing.preProcess(text))));
    }
    
}
