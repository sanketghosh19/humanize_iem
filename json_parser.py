import json
import os

# Function to process a single JSON file and convert it to the desired structure
def process_json_file(input_file_path, output_directory):
    # Load the input JSON file
    with open(input_file_path, 'r') as file:
        data = json.load(file)

    # Prepare the structured data
    structured_data = {"words": []}
    for block in data.get("Blocks", []):
        if block.get("BlockType") == "WORD":
            text = block.get("Text", "")
            bounding_box = block.get("Geometry", {}).get("BoundingBox", {})
            structured_data["words"].append({
                "text": text,
                "boundingBox": {
                    "x": bounding_box.get("Left", 0) * 1000,  # Scale values for better readability
                    "y": bounding_box.get("Top", 0) * 1000,
                    "width": bounding_box.get("Width", 0) * 1000,
                    "height": bounding_box.get("Height", 0) * 1000
                }
            })

    # Generate a unique output file name based on the input file name
    base_name = os.path.basename(input_file_path).replace('.json', '')
    output_file_name = f"{base_name}_structured.json"
    output_file_path = os.path.join(output_directory, output_file_name)

    # Save the structured data to the output file
    with open(output_file_path, 'w') as output_file:
        json.dump(structured_data, output_file, indent=4)

    print(f"Processed: {input_file_path} -> {output_file_path}")

# Function to process multiple JSON files in a directory
def process_multiple_json_files(input_directory, output_directory):
    # Ensure the output directory exists
    os.makedirs(output_directory, exist_ok=True)

    # Iterate through all JSON files in the input directory
    for file_name in os.listdir(input_directory):
        if file_name.endswith('.json'):
            input_file_path = os.path.join(input_directory, file_name)
            process_json_file(input_file_path, output_directory)

if __name__ == "__main__":
    # Input directory containing JSON files
    input_directory = "json_input"
    
    # Output directory to save the structured files
    output_directory = "json_output"

    # Process all JSON files
    process_multiple_json_files(input_directory, output_directory)
