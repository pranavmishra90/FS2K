//Comma for csv, "\t" for tsv
def delim = ","
// Flag: if true, overwrite any existing measurement; if false, only add new ones
boolean overwriteExisting = false  

def Aproject = getProject()
projectDir = new File(buildFilePath(PROJECT_BASE_DIR))

// Prompt for the single CSV file to import
def csvFile = qupath.fx.utils.FXUtils.callOnApplicationThread {
    FileChoosers.buildFileChooser()
                .initialDirectory(projectDir)
                .extensionFilters(FileChoosers.createExtensionFilter("CSV File", "*.csv", ".csv"))
                .build()
                .showOpenDialog()
}

if (!csvFile) {
    println "No CSV file selected. Exiting."
    return
}



// Read all lines of the CSV file
def lines = new File(csvFile.getAbsolutePath()).readLines()
if (lines.size() < 2) {
    println "CSV file is empty or does not have enough rows."
    return
}

// The first line is assumed to be the header row
def header = lines[0].split(delim)

// Ensure required columns are present
if (!header.contains("Image") || !header.contains("Object ID")) {
    println "CSV file must contain both 'Image' and 'Object ID' columns."
    return
}

// Pre-check which columns (besides "Image" and "Object ID") are numeric,
// using the first data row as representative.
def firstDataRow = lines[1].split(delim)
def numericColumns = []
header.eachWithIndex { col, i ->
    if (col != 'Image' && col != 'Object ID') {
         try {
              double testVal = firstDataRow[i] as double
              numericColumns << col
         } catch(Exception e) {
              // Not numeric – ignore this column.
         }
    }
}
println "Columns to import: ${numericColumns}"

// Group CSV rows by the value in the "Image" column
def rowsByImage = [:].withDefault { [] }
lines.drop(1).each { line ->
    def cols = line.split(delim)
    if (cols.size() != header.size()) {
         println "Skipping line due to mismatch in number of columns: $line"
         return
    }
    def rowMap = lineToMap(header, cols)
    def imageName = rowMap['Image']
    rowsByImage[imageName] << rowMap
}

// Process only those images that have rows in the CSV
rowsByImage.each { imageName, rows ->
    println "Processing image: ${imageName}"
    // Look up the image entry in the project – ensure the CSV image name matches exactly!
    def entry = Aproject.getImageList().find { it.getImageName() == imageName }
    if (entry == null) {
         println "NO ENTRY FOUND FOR IMAGE: " + imageName
         return // skip processing for this image
    }
    
    // Get the image data and its object hierarchy
    def imageData = entry.readImageData()
    def hierarchy = imageData.getHierarchy()
    
    // Get all detection objects 
    def cells = hierarchy.getDetectionObjects()
    
    // Group the cells by their unique ID (using getID())
    def cellsByID = cells.groupBy { it.getID().toString() }
    
     // For each CSV row for this image, find the matching cell by "Object ID"
    rows.each { row ->
         def objectId = row['Object ID']
         def cellGroup = cellsByID[objectId]
         if (cellGroup == null) {
             println "WARN: No cell found for Object ID ${objectId} in image ${imageName}"
         } else if (cellGroup.size() != 1) {
             println "WARN: ${cellGroup.size()} cells found for Object ID ${objectId} in image ${imageName} - skipping"
         } else {
             def cell = cellGroup[0]
             // For each numeric column, update the measurement if allowed
             numericColumns.each { col ->
                 try {
                     double numVal = row[col] as double
                     // Check using the measurement list; if overwrite is enabled or the measurement doesn't exist, update it.
                     if (overwriteExisting || !cell.getMeasurementList().containsKey(col)) {
                         cell.getMeasurementList().put(col, numVal)
                         //println "Writing measurement '${col}' with value ${numVal} for Object ID ${objectId} in image ${imageName}"
                     }
                 } catch(Exception e) {
                     println "Error processing column '${col}' for Object ID '${objectId}' in image ${imageName}: ${e}"
                 }
             }
         }
    }
    // Save modifications to the image data
    entry.saveImageData(imageData)
}
println "Done with CSV import!"

// Helper function: convert a row (array of strings) into a Map using the header.
Map lineToMap(String[] header, String[] content) {
    def map = [:]
    if (header.size() != content.size()) {
        throw new IllegalArgumentException("Header length doesn't match the content length!")
    }
    for (int i = 0; i < header.size(); i++) {
         map[header[i]] = content[i]
    }
    return map
}
