# Parse Textrtac output and save the results in json file

import boto3
import json
import os
from dotenv import load_dotenv
from trp import Document

# Load environment variables
load_dotenv()

class TextractLayoutExtractor:
    def __init__(self):
        self.client = boto3.client(
            "textract",
            aws_access_key_id=os.getenv("AWS_ACCESS_KEY_ID"),
            aws_secret_access_key=os.getenv("AWS_SECRET_ACCESS_KEY"),
            region_name=os.getenv("AWS_REGION"),
        )
        self.original_results = None

    def get_bounding_box_info(self, geometry):
        """Helper function to format bounding box information"""
        if not geometry:
            return None
        
        bbox = geometry.boundingBox
        polygon = geometry.polygon
        
        return {
            "bounding_box": {
                "width": bbox.width,
                "height": bbox.height,
                "left": bbox.left,
                "top": bbox.top
            },
            "polygon": [
                {"x": point.x, "y": point.y}
                for point in polygon
            ]
        }

    def process_file(self, file_path):
        with open(file_path, "rb") as file:
            file_bytes = file.read()
            
        response = self.client.analyze_document(
            Document={"Bytes": file_bytes}, FeatureTypes=["TABLES", "FORMS"]
        )
        
        self.original_results = response
        
        doc = Document(response)
        formatted_output = {"pages": []}
        
        for page_num, page in enumerate(doc.pages, 1):
            page_data = {
                "page_number": page_num,
                "content": {
                    "lines": [],
                    "tables": [],
                    "forms": []
                }
            }
            
            # Process lines and words
            for line in page.lines:
                line_data = {
                    "text": line.text,
                    #"confidence": line.confidence,
                    "geometry": self.get_bounding_box_info(line.geometry),
                    "words": []
                }
                
                for word in line.words:
                    word_data = {
                        "text": word.text,
                        #"confidence": word.confidence,
                        "geometry": self.get_bounding_box_info(word.geometry)
                    }
                    line_data["words"].append(word_data)
                
                page_data["content"]["lines"].append(line_data)
            
            # Process tables
            for table_num, table in enumerate(page.tables, 1):
                table_data = {
                    "table_number": table_num,
                    "rows": []
                }
                
                for r, row in enumerate(table.rows):
                    row_data = []
                    for c, cell in enumerate(row.cells):
                        cell_data = {
                            "text": cell.text,
                            #"confidence": cell.confidence,
                            "geometry": self.get_bounding_box_info(cell.geometry),
                            "row_index": r,
                            "column_index": c
                        }
                        row_data.append(cell_data)
                    
                    table_data["rows"].append(row_data)
                page_data["content"]["tables"].append(table_data)
            
            # Process forms
            for field in page.form.fields:
                field_data = {
                    "key": {
                        "text": field.key.text,
                        "geometry": self.get_bounding_box_info(field.key.geometry)
                    }
                }
                
                if field.value:
                    field_data["value"] = {
                        "text": field.value.text,
                        "geometry": self.get_bounding_box_info(field.value.geometry)
                    }
                
                page_data["content"]["forms"].append(field_data)
            
            formatted_output["pages"].append(page_data)
        
        return formatted_output

    def save_results(self, layout_data, output_file):
        # Save JSON data           
        with open(output_file, "w", encoding='utf-8') as file:
            json.dump(layout_data, file, indent=4)

# Usage example
if __name__ == "__main__":
    extractor = TextractLayoutExtractor()
    file_path = "template_sb1.pdf"  # Replace with your image or PDF path
    layout = extractor.process_file(file_path)
    #extractor.save_results(extractor.original_results, "layout_original_results.json")
    extractor.save_results(layout, "layout_simplified_results.json")
