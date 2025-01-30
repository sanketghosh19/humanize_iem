import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.kernel.geom.PageSize;
//import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.property.UnitValue;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.property.AreaBreakType;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class TextractPdfGenerator {
    private static final float POINTS_PER_INCH = 72;
    private static final float DEFAULT_PAGE_WIDTH = PageSize.A4.getWidth();
    private static final float DEFAULT_PAGE_HEIGHT = PageSize.A4.getHeight();

    public void generatePdf(String jsonFilePath, String outputPdfPath) throws IOException {
        // Read JSON file
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> jsonData = mapper.readValue(new File(jsonFilePath), Map.class);

        // Create PDF
        PdfWriter writer = new PdfWriter(outputPdfPath);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        // Process each page in the JSON
        for (Map<String, Object> page : (java.util.List<Map<String, Object>>) jsonData.get("pages")) {
            Map<String, Object> content = (Map<String, Object>) page.get("content");
            
            // Process lines
            processLines(document, (java.util.List<Map<String, Object>>) content.get("lines"));
            
            // Process tables
            processTables(document, (java.util.List<Map<String, Object>>) content.get("tables"));
            
            // Process forms
            processForms(document, (java.util.List<Map<String, Object>>) content.get("forms"));
            
            // Add new page if there are more pages
            if (page != ((java.util.List<Map<String, Object>>) jsonData.get("pages")).get(((java.util.List<Map<String, Object>>) jsonData.get("pages")).size() - 1)) {
                document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
            }
        }

        document.close();
    }

    private void processLines(Document document, java.util.List<Map<String, Object>> lines) {
        if (lines == null) return;

        for (Map<String, Object> line : lines) {
            Map<String, Object> geometry = (Map<String, Object>) line.get("geometry");
            Map<String, Object> boundingBox = (Map<String, Object>) geometry.get("bounding_box");
            
            // Convert relative positions to absolute positions
            float left = ((Number) boundingBox.get("left")).floatValue() * DEFAULT_PAGE_WIDTH;
            float top = ((Number) boundingBox.get("top")).floatValue() * DEFAULT_PAGE_HEIGHT;
            
            Paragraph paragraph = new Paragraph((String) line.get("text"))
                .setFixedPosition(left, DEFAULT_PAGE_HEIGHT - top, DEFAULT_PAGE_WIDTH)
                .setFontSize(12);
            
            document.add(paragraph);
        }
    }

    private void processTables(Document document, java.util.List<Map<String, Object>> tables) {
        if (tables == null) return;

        for (Map<String, Object> tableData : tables) {
            java.util.List<java.util.List<Map<String, Object>>> rows = 
                (java.util.List<java.util.List<Map<String, Object>>>) tableData.get("rows");
            
            if (rows.isEmpty()) continue;
            
            // Create table with the correct number of columns
            Table table = new Table(UnitValue.createPointArray(new float[rows.get(0).size()]));
            
            // Process each row
            for (java.util.List<Map<String, Object>> row : rows) {
                for (Map<String, Object> cell : row) {
                    Cell pdfCell = new Cell().add(new Paragraph((String) cell.get("text")));
                    
                    // Get cell geometry if needed for specific positioning
                    Map<String, Object> geometry = (Map<String, Object>) cell.get("geometry");
                    Map<String, Object> boundingBox = (Map<String, Object>) geometry.get("bounding_box");
                    
                    table.addCell(pdfCell);
                }
            }
            
            // Position the table using the first cell's position
            Map<String, Object> firstCellGeometry = (Map<String, Object>) rows.get(0).get(0).get("geometry");
            Map<String, Object> boundingBox = (Map<String, Object>) firstCellGeometry.get("bounding_box");
            float left = ((Number) boundingBox.get("left")).floatValue() * DEFAULT_PAGE_WIDTH;
            float top = ((Number) boundingBox.get("top")).floatValue() * DEFAULT_PAGE_HEIGHT;
            
            table.setFixedPosition(left, DEFAULT_PAGE_HEIGHT - top, DEFAULT_PAGE_WIDTH);
            document.add(table);
        }
    }

    private void processForms(Document document, java.util.List<Map<String, Object>> forms) {
        if (forms == null) return;

        for (Map<String, Object> form : forms) {
            Map<String, Object> key = (Map<String, Object>) form.get("key");
            Map<String, Object> value = (Map<String, Object>) form.get("value");
            
            // Process key
            Map<String, Object> keyGeometry = (Map<String, Object>) key.get("geometry");
            Map<String, Object> keyBox = (Map<String, Object>) keyGeometry.get("bounding_box");
            float keyLeft = ((Number) keyBox.get("left")).floatValue() * DEFAULT_PAGE_WIDTH;
            float keyTop = ((Number) keyBox.get("top")).floatValue() * DEFAULT_PAGE_HEIGHT;
            
            Paragraph keyParagraph = new Paragraph((String) key.get("text"))
                .setFixedPosition(keyLeft, DEFAULT_PAGE_HEIGHT - keyTop, DEFAULT_PAGE_WIDTH)
                .setFontSize(12)
                .setBold();
            
            document.add(keyParagraph);
            
            // Process value if present
            if (value != null) {
                Map<String, Object> valueGeometry = (Map<String, Object>) value.get("geometry");
                Map<String, Object> valueBox = (Map<String, Object>) valueGeometry.get("bounding_box");
                float valueLeft = ((Number) valueBox.get("left")).floatValue() * DEFAULT_PAGE_WIDTH;
                float valueTop = ((Number) valueBox.get("top")).floatValue() * DEFAULT_PAGE_HEIGHT;
                
                Paragraph valueParagraph = new Paragraph((String) value.get("text"))
                    .setFixedPosition(valueLeft, DEFAULT_PAGE_HEIGHT - valueTop, DEFAULT_PAGE_WIDTH)
                    .setFontSize(12);
                
                document.add(valueParagraph);
            }
        }
    }

    // Usage example
    public static void main(String[] args) {
        TextractPdfGenerator generator = new TextractPdfGenerator();
        try {
            generator.generatePdf("layout_simplified_results.json", "output.pdf");
            System.out.println("PDF generated successfully!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
