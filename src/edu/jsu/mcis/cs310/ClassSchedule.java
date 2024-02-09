package edu.jsu.mcis.cs310;

import com.github.cliftonlabs.json_simple.*;
import com.opencsv.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Iterator;

public class ClassSchedule {

    private final String CSV_FILENAME = "jsu_sp24_v1.csv";
    private final String JSON_FILENAME = "jsu_sp24_v1.json";

    private final String CRN_COL_HEADER = "crn";
    private final String SUBJECT_COL_HEADER = "subject";
    private final String NUM_COL_HEADER = "num";
    private final String DESCRIPTION_COL_HEADER = "description";
    private final String SECTION_COL_HEADER = "section";
    private final String TYPE_COL_HEADER = "type";
    private final String CREDITS_COL_HEADER = "credits";
    private final String START_COL_HEADER = "start";
    private final String END_COL_HEADER = "end";
    private final String DAYS_COL_HEADER = "days";
    private final String WHERE_COL_HEADER = "where";
    private final String SCHEDULE_COL_HEADER = "schedule";
    private final String INSTRUCTOR_COL_HEADER = "instructor";
    private final String SUBJECTID_COL_HEADER = "subjectid";

    public String convertCsvToJsonString(List<String[]> csv) {
        Iterator<String[]> csvRecordIterator = csv.iterator();

        String jsonString = "";
        String[] headers;

        if (csvRecordIterator.hasNext()) {
            // Headers: "crn subject num description section type credits start end days where schedule instructor"
            headers = csvRecordIterator.next();

            // Finding indexes
            HashMap<String, Integer> headerIndexes = new HashMap<>();

            for (int i=0; i < headers.length; ++i) {
                headerIndexes.put(headers[i], i);
            }

            // For scheduletype
            JsonObject scheduletypeMap = new JsonObject();

            // For subject
            JsonObject subjectMap = new JsonObject();

            // For course
            JsonObject courseMap = new JsonObject();

            // For section
            JsonArray sectionsArray = new JsonArray();


            while (csvRecordIterator.hasNext()) {
                String[] csvRecord = csvRecordIterator.next();

                // scheduletype construction
                String type = csvRecord[headerIndexes.get(TYPE_COL_HEADER)];
                String schedule = csvRecord[headerIndexes.get(SCHEDULE_COL_HEADER)];
                if (scheduletypeMap.get(type) == null) { // Has the course not been added?
                    scheduletypeMap.put(type, schedule);
                }

                // subject construction
                String subjectID = csvRecord[headerIndexes.get(NUM_COL_HEADER)].replaceAll("\\d", "").replaceAll("\\s", "");
                if (subjectMap.get(subjectID) == null) { // Has the course not been added?
                    String subjectHeader = csvRecord[headerIndexes.get(SUBJECT_COL_HEADER)];
                    subjectMap.put(subjectID,subjectHeader);
                }

                // Course construction
                String num = csvRecord[headerIndexes.get(NUM_COL_HEADER)];
                String numWithoutLetters = num.replaceAll("[A-Z]", "").replaceAll("\\s", "");
                if (courseMap.get(num) == null) { // Has the course not been added?
                    String description = csvRecord[headerIndexes.get(DESCRIPTION_COL_HEADER)];
                    int credits = Integer.parseInt(csvRecord[headerIndexes.get(CREDITS_COL_HEADER)]);
                    JsonObject course = new JsonObject();
                    course.put(SUBJECTID_COL_HEADER, subjectID);
                    course.put(NUM_COL_HEADER, numWithoutLetters);
                    course.put(DESCRIPTION_COL_HEADER, description);
                    course.put(CREDITS_COL_HEADER, credits);
                    courseMap.put(num, course);
                }

                // Section Construction
                int crn = Integer.parseInt(csvRecord[headerIndexes.get(CRN_COL_HEADER)]);
                String sectionHeader = csvRecord[headerIndexes.get(SECTION_COL_HEADER)];
                String start = csvRecord[headerIndexes.get(START_COL_HEADER)];
                String end = csvRecord[headerIndexes.get(END_COL_HEADER)];
                String days = csvRecord[headerIndexes.get(DAYS_COL_HEADER)];
                String where = csvRecord[headerIndexes.get(WHERE_COL_HEADER)];
                String instructorsAllTogether = csvRecord[headerIndexes.get(INSTRUCTOR_COL_HEADER)];

                JsonObject section = new JsonObject();

                section.put(CRN_COL_HEADER, crn);
                section.put(SUBJECTID_COL_HEADER, subjectID);
                section.put(NUM_COL_HEADER, numWithoutLetters);
                section.put(SECTION_COL_HEADER, sectionHeader);
                section.put(TYPE_COL_HEADER, type);
                section.put(START_COL_HEADER, start);
                section.put(END_COL_HEADER, end);
                section.put(DAYS_COL_HEADER, days);
                section.put(WHERE_COL_HEADER, where);

                // Splitting instructors up and putting into array
                List<String> instructors = Arrays.asList(instructorsAllTogether.split(", "));
                JsonArray instructorArray = new JsonArray();

                for (String person : instructors) {
                    instructorArray.add(person);
                }

                section.put(INSTRUCTOR_COL_HEADER, instructorArray);
                sectionsArray.add(section);


            }

            // Creating "outer" container for json data
            JsonObject courseListMap = new JsonObject();
            courseListMap.put("scheduletype", scheduletypeMap);
            courseListMap.put("subject" ,subjectMap);
            courseListMap.put("course" ,courseMap);
            courseListMap.put("section", sectionsArray);

            jsonString = Jsoner.serialize(courseListMap);

       }

        return jsonString;
    }

    public String convertJsonToCsvString(JsonObject json) {
        
        StringWriter writer = new StringWriter();
        CSVWriter csvWriter = new CSVWriter(writer, '\t', '"', '\\', "\n");
        String[] header = {CRN_COL_HEADER, SUBJECT_COL_HEADER, NUM_COL_HEADER, DESCRIPTION_COL_HEADER, SECTION_COL_HEADER, TYPE_COL_HEADER,
                           CREDITS_COL_HEADER, START_COL_HEADER, END_COL_HEADER, DAYS_COL_HEADER, WHERE_COL_HEADER, SCHEDULE_COL_HEADER,
                           INSTRUCTOR_COL_HEADER};
        csvWriter.writeNext(header);

        JsonObject scheduletype = (JsonObject)json.get("scheduletype");
        JsonObject subject = (JsonObject)json.get("subject");
        JsonObject course = (JsonObject)json.get("course");
        JsonArray section = (JsonArray)json.get("section");
        
        int csvNumRows = section.size();
        
        for (int i=0; i < csvNumRows; i++) {

            // via section
            JsonObject sectionMap = section.getMap(i);
            BigDecimal crn = (BigDecimal)sectionMap.get(CRN_COL_HEADER);
            String subjectid = (String)sectionMap.get(SUBJECTID_COL_HEADER);
            String numNoLetters = (String)sectionMap.get(NUM_COL_HEADER);
            String num = subjectid + " " + numNoLetters;                  
            String sectionHeader = (String)sectionMap.get(SECTION_COL_HEADER);
            String type = (String)sectionMap.get(TYPE_COL_HEADER);
            String start = (String)sectionMap.get(START_COL_HEADER);
            String end = (String)sectionMap.get(END_COL_HEADER);
            String days = (String)sectionMap.get(DAYS_COL_HEADER);
            String where = (String)sectionMap.get(WHERE_COL_HEADER);
            JsonArray instructorArray = (JsonArray)sectionMap.get(INSTRUCTOR_COL_HEADER);
            
            String instructor = instructorArray.toString();
            instructor = instructor.substring(1, instructor.length() - 1);
            
            // via scheduletype
            String schedule = (String)scheduletype.get(type);
 
            // Via course
            JsonObject courseMap = (JsonObject)course.get(num);
            BigDecimal credits = (BigDecimal)courseMap.get(CREDITS_COL_HEADER);
            String description = (String)courseMap.get(DESCRIPTION_COL_HEADER);
            
            // Via subject
            String subjectHeader = (String)subject.get(subjectid);
            
            // Convert crn and credits to string
            String crnHeader = String.valueOf(crn);
            String creditsHeader = String.valueOf(credits);
            
            // Adding everything in order
            String[] record = {crnHeader, subjectHeader, num, description, sectionHeader, type, creditsHeader, start, end, days, where, 
                               schedule, instructor};
            csvWriter.writeNext(record);
        }
        
        String csvString = writer.toString();

        return csvString; 

    }

    public JsonObject getJson() {

        JsonObject json = getJson(getInputFileData(JSON_FILENAME));
        return json;

    }

    public JsonObject getJson(String input) {

        JsonObject json = null;

        try {
            json = (JsonObject)Jsoner.deserialize(input);
        }
        catch (Exception e) { e.printStackTrace(); }

        return json;

    }

    public List<String[]> getCsv() {

        List<String[]> csv = getCsv(getInputFileData(CSV_FILENAME));
        return csv;

    }

    public List<String[]> getCsv(String input) {

        List<String[]> csv = null;

        try {

            CSVReader reader = new CSVReaderBuilder(new StringReader(input)).withCSVParser(new CSVParserBuilder().withSeparator('\t').build()).build();
            csv = reader.readAll();

        }
        catch (Exception e) { e.printStackTrace(); }

        return csv;

    }

    public String getCsvString(List<String[]> csv) {

        StringWriter writer = new StringWriter();
        CSVWriter csvWriter = new CSVWriter(writer, '\t', '"', '\\', "\n");

        csvWriter.writeAll(csv);

        return writer.toString();

    }

    private String getInputFileData(String filename) {

        StringBuilder buffer = new StringBuilder();
        String line;

        ClassLoader loader = ClassLoader.getSystemClassLoader();

        try {

            BufferedReader reader = new BufferedReader(new InputStreamReader(loader.getResourceAsStream("resources" + File.separator + filename)));

            while((line = reader.readLine()) != null) {
                buffer.append(line).append('\n');
            }

        }
        catch (Exception e) { e.printStackTrace(); }

        return buffer.toString();

    }

}