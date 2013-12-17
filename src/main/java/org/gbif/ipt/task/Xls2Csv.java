package org.gbif.ipt.task;

import org.gbif.ipt.utils.ActionLogger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.sibcolombia.sibsp.action.BaseAction;
import net.sibcolombia.sibsp.action.portal.CreateResourceAction;
import net.sibcolombia.sibsp.configuration.ApplicationConfig;
import net.sibcolombia.sibsp.configuration.Constants;
import net.sibcolombia.sibsp.configuration.DataDir;
import net.sibcolombia.sibsp.model.Resource;
import net.sibcolombia.sibsp.service.BaseManager;
import net.sibcolombia.sibsp.struts2.SimpleTextProvider;
import org.apache.log4j.Logger;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.exceptions.InvalidOperationException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

@Singleton
public class Xls2Csv extends BaseManager {

  // logging
  private static final Logger log = Logger.getLogger(CreateResourceAction.class);

  private final Map<String, ArrayList<String>> columnCoreOcurrenceMapping =
    initializeColumnMapping(Constants.DWC_CORE_OCURRENCE_COLUMN_MAPPING_FILE);
  private final Map<String, ArrayList<String>> columnCoreTaxonomicMapping =
    initializeColumnMapping(Constants.DWC_CORE_TAXONOMIC_COLUMN_MAPPING_FILE);
  private final Map<String, ArrayList<String>> columnCoreOcurrenceRelationshipAndMeasurementMapping =
    initializeColumnMapping(Constants.DWC_CORE_OCURRENCE_COLUMN_MAPPING_FILE,
      Constants.DWC_EXTENSION_RESOURCE_RELATIONSHIP_COLUMN_MAPPING_FILE,
      Constants.DWC_EXTENSION_MEASUREMENT_OR_FACTS_COLUMN_MAPPING_FILE);


  // create instance of BaseAction - allows class to retrieve i18n terms via getText()
  private final BaseAction baseAction;

  @Inject
  public Xls2Csv(ApplicationConfig config, DataDir dataDir, SimpleTextProvider simpleTextProvider) {
    super(config, dataDir);
    baseAction = new BaseAction(simpleTextProvider, config);
  }

  public File convertExcelCoreBasicToCsv(Resource resource, File sourceFile, ActionLogger actionLogger)
    throws IOException, InvalidFormatException {
    Workbook template = WorkbookFactory.create(sourceFile);
    File file = dataDir.sourceExcelFile(resource, "data");
    //FileWriter csvFile = new FileWriter(file);
    //CSVWriter writer = new CSVWriter(csvFile, '\t', CSVWriter.NO_QUOTE_CHARACTER);
    OutputStream csvFile = new FileOutputStream(file);
    CSVWriter writer = new CSVWriter(new OutputStreamWriter(csvFile, "UTF-8"), '\t', CSVWriter.NO_QUOTE_CHARACTER);

    // Map of required columns
    Map<Integer, String> columnLocationRequiredElements = new HashMap<Integer, String>();

    // Read first row columns names
    Sheet sheet = template.getSheet("Elementos mínimos");
    int totalColumns = sheet.getRow(0).getLastCellNum();
    int columnsWithRequiredElements = 0;
    Iterator<Row> rowIterator = sheet.rowIterator();
    String[] entries = new String[totalColumns];
    Row row = sheet.getRow(0);
    for (int counter = 0; counter < totalColumns; counter++) {
      if (!readCellValue(row.getCell(counter, Row.CREATE_NULL_AS_BLANK)).isEmpty()) {
        if (columnCoreOcurrenceMapping.containsKey(readCellValue(row.getCell(counter, Row.CREATE_NULL_AS_BLANK)))) {
          entries[counter] =
            columnCoreOcurrenceMapping.get(readCellValue(row.getCell(counter, Row.CREATE_NULL_AS_BLANK))).get(0);
          log.info("counterColumn: "+ counter+ " : entries[counter]");
          log.info(" value: "+ columnCoreOcurrenceMapping.get(readCellValue(row.getCell(counter, Row.CREATE_NULL_AS_BLANK))));
          if (columnCoreOcurrenceMapping.get(readCellValue(row.getCell(counter, Row.CREATE_NULL_AS_BLANK))).get(1)
            .equalsIgnoreCase("required")) {
            // This column of template couldn't be empty, so map it
            columnLocationRequiredElements.put(counter,
              columnCoreOcurrenceMapping.get(readCellValue(row.getCell(counter, Row.CREATE_NULL_AS_BLANK))).get(0));
            columnsWithRequiredElements++;
          }else{
        	  log.info("No es requerido"+columnCoreOcurrenceMapping.get(readCellValue(row.getCell(counter, Row.CREATE_NULL_AS_BLANK))).get(1)); 
          }
        }
      } else {
        entries[counter] = "";
      }
    }
    //si el número de columnas requeridas es diferente del establecido en el archivo de configuración
    if (columnsWithRequiredElements != Integer
      .valueOf(columnCoreOcurrenceMapping.get("Total required elements").get(0))) {
      throw new InvalidFormatException(baseAction.getText("sibsp.application.portal.error.with.template.elements.data"));
    }
    writer.writeNext(entries);
    
    for (int i=0; i < entries.length;i++){
    	log.info(entries[i]);
    }

    while (rowIterator.hasNext()) {
      row = rowIterator.next();
      if (row.getRowNum() != 0) {
    	  log.info("totalColumns: "+ totalColumns);
        for (int counter = 0; counter < totalColumns; counter++) {
          log.info("Fila: " + row.getRowNum() + " Columna: " + counter);
          if (!readCellValue(row.getCell(counter, Row.CREATE_NULL_AS_BLANK)).isEmpty()) {
            entries[counter] = readCellValue(row.getCell(counter, Row.CREATE_NULL_AS_BLANK));
          } else {
            if (columnLocationRequiredElements.containsKey(counter)) {
              throw new InvalidFormatException(baseAction.getTextWithDynamicArgs(
                "sibsp.application.portal.error.element.notempty", columnLocationRequiredElements.get(counter)));
            } else {
              entries[counter] = "";
            }
          }
        }
        writer.writeNext(entries);
      }
    }
    writer.close();
    return file;
  }
  
  public File convertExcelCoreCompleteToCsv(Resource resource, File sourceFile, ActionLogger actionLogger)
		    throws IOException, InvalidFormatException {
		    Workbook template = WorkbookFactory.create(sourceFile);
		    File file = dataDir.sourceExcelFile(resource, "data");
		    OutputStream csvFile = new FileOutputStream(file);
		    CSVWriter writer = new CSVWriter(new OutputStreamWriter(csvFile, "UTF-8"), '\t', CSVWriter.NO_QUOTE_CHARACTER);

		    // Map of required columns
		    Map<Integer, String> columnLocationRequiredElements = new HashMap<Integer, String>();

		    // Read first row columns names
		    Sheet sheet = template.getSheet("Plantilla");
		    int totalColumns = sheet.getRow(0).getLastCellNum();
		    log.info("Num of cols: "+totalColumns);
		    int columnsWithRequiredElements = 0;
		    Iterator<Row> rowIterator = sheet.rowIterator();
		    String[] entries = new String[totalColumns];
		    Row row = sheet.getRow(0);
		    for (int counter = 0; counter < totalColumns; counter++) {
		      if (!readCellValue(row.getCell(counter, Row.CREATE_NULL_AS_BLANK)).isEmpty()) {
		        if (columnCoreOcurrenceRelationshipAndMeasurementMapping.containsKey(readCellValue(row.getCell(counter,
		          Row.CREATE_NULL_AS_BLANK)))) {
		          entries[counter] =
		            columnCoreOcurrenceRelationshipAndMeasurementMapping.get(
		              readCellValue(row.getCell(counter, Row.CREATE_NULL_AS_BLANK))).get(0);
		          if (columnCoreOcurrenceRelationshipAndMeasurementMapping.get(readCellValue(row.getCell(counter, Row.CREATE_NULL_AS_BLANK))).get(1).equalsIgnoreCase("required")) {
		            // This column of template couldn't be empty, so map it
		            columnLocationRequiredElements.put(
		              counter,
		              columnCoreOcurrenceRelationshipAndMeasurementMapping.get(
		                readCellValue(row.getCell(counter, Row.CREATE_NULL_AS_BLANK))).get(0));
		            columnsWithRequiredElements++;
		          }
		        }
		      } else {
		        entries[counter] = "";
		      }
		    }
		    
		    if (columnsWithRequiredElements != Integer.valueOf(columnCoreOcurrenceRelationshipAndMeasurementMapping.get(
		      "Total required elements").get(0))) {
		      throw new InvalidFormatException(baseAction.getText("sibsp.application.portal.error.with.template.elements.data"));
		    }
		    writer.writeNext(entries);

		    int[] elmMiss=reviewSpreedsheet(resource, sourceFile, actionLogger);
		    
		    String errE="";
		    Row rowOne=sheet.getRow(1);
		    for(int j=0; j<elmMiss.length;j++){
		    	if(elmMiss[j]!=0){
		    		errE=errE+" "+readCellValue(rowOne.getCell(j, Row.CREATE_NULL_AS_BLANK))+" Columna: "+(j+1)+" Número de celdas vacias: "+elmMiss[j]+"\n";
		    	}
		    }
		    
		    if(!errE.equals("")){
		    	throw new InvalidOperationException(errE);
		    }
		    
		    while (rowIterator.hasNext()) {
		      row = rowIterator.next();
		      if (row.getRowNum() != 0 && row.getRowNum() != 1) {
		        for (int counter = 0; counter < totalColumns; counter++) {
		          if (!readCellValue(row.getCell(counter, Row.CREATE_NULL_AS_BLANK)).isEmpty()) {
		            entries[counter] = readCellValue(row.getCell(counter, Row.CREATE_NULL_AS_BLANK));
		          } else { 
		        	if (columnLocationRequiredElements.containsKey(counter)) {
		        		throw new InvalidFormatException(baseAction.getTextWithDynamicArgs("sibsp.application.portal.error.element.notempty", columnLocationRequiredElements.get(counter)));
		    		} else {
		    			entries[counter] = "";
		    		}
		          }
		        }

		        writer.writeNext(entries);
		      }
		    }

		    writer.close();
		    return file;
		  }

  
private int[] reviewSpreedsheet(Resource resource, File sourceFile, ActionLogger actionLogger) throws IOException, InvalidFormatException{
	Workbook template = WorkbookFactory.create(sourceFile);
	File file = dataDir.sourceExcelFile(resource, "data");
	//OutputStream csvFile = new FileOutputStream(file);
	
	// Map of required columns
    Map<Integer, String> columnLocationRequiredElements = new HashMap<Integer, String>();
    // Read first row columns names
    Sheet sheet = template.getSheet("Plantilla");
    int totalColumns = sheet.getRow(0).getLastCellNum();
    int columnsWithRequiredElements = 0;
    Iterator<Row> rowIterator = sheet.rowIterator();
    String[] entries = new String[totalColumns];
    Row row = sheet.getRow(0);
    
    for(int counter = 0; counter < totalColumns; counter++){
    	if(!readCellValue(row.getCell(counter, Row.CREATE_NULL_AS_BLANK)).isEmpty()){
    		if(columnCoreOcurrenceRelationshipAndMeasurementMapping.containsKey(readCellValue(row.getCell(counter,Row.CREATE_NULL_AS_BLANK)))){
    			entries[counter] =columnCoreOcurrenceRelationshipAndMeasurementMapping.get(readCellValue(row.getCell(counter, Row.CREATE_NULL_AS_BLANK))).get(0);
    			if(columnCoreOcurrenceRelationshipAndMeasurementMapping.get(readCellValue(row.getCell(counter, Row.CREATE_NULL_AS_BLANK))).get(1).equalsIgnoreCase("required")){
    				columnLocationRequiredElements.put(counter,columnCoreOcurrenceRelationshipAndMeasurementMapping.get(readCellValue(row.getCell(counter, Row.CREATE_NULL_AS_BLANK))).get(0));
    				columnsWithRequiredElements++;
    			}
    		}
    	}else{
    		entries[counter] = "";
    	}
    }
    
    if(columnsWithRequiredElements != Integer.valueOf(columnCoreOcurrenceRelationshipAndMeasurementMapping.get("Total required elements").get(0))){
    	throw new InvalidFormatException(baseAction.getText("sibsp.application.portal.error.with.template.elements.data"));
    }
    
    
    Row rowZero=sheet.getRow(0);
    int[] missEl = new int[totalColumns];
    //String missE="";
    boolean isFull=false;
    for(int in=0; in<totalColumns; in++){
		missEl[in]=0;
	}
    
    int position=-1;
    
    for(int k=0; k < totalColumns; k++){
		if(readCellValue(rowZero.getCell(k, Row.CREATE_NULL_AS_BLANK)).equals("dataGeneralizations")){
			position=k;
		}
	}
    
    while(rowIterator.hasNext()){
    	row = rowIterator.next();
    	isFull=false;
    	if(position!=-1){
    		if(!readCellValue(row.getCell(position, Row.CREATE_NULL_AS_BLANK)).isEmpty()){
    			isFull=true;
    		}
    	}
    	
    	if(row.getRowNum() != 0 && row.getRowNum() != 1){
    		for(int counter = 0; counter < totalColumns; counter++){
    			if(!readCellValue(row.getCell(counter, Row.CREATE_NULL_AS_BLANK)).trim().isEmpty()){
    				System.out.println(readCellValue(row.getCell(counter, Row.CREATE_NULL_AS_BLANK))); //*********
    			}else{
    				String colmN=readCellValue(rowZero.getCell(counter, Row.CREATE_NULL_AS_BLANK));
    				if(isFull){
    					if((colmN.equals("locality")||colmN.equals("decimalLatitude"))||(colmN.equals("decimalLongitude"))){
    						System.out.println("No se revisa"); //*********
    					}else{
    						if(columnLocationRequiredElements.containsKey(counter)){
    							missEl[counter]++;
    						}
    					}
    				}else{
    					if(columnLocationRequiredElements.containsKey(counter)){
    						missEl[counter]++;
    					}
    				}
    			}
    		}
    	}
    }
    
    return missEl;
}

private void extracted(String missE) throws InvalidFormatException {
	throw new InvalidFormatException(baseAction.getTextWithDynamicArgs("sibsp.application.portal.error.element.notempty", missE));
}

  public File convertExcelTaxonomicToCsv(Resource resource, File sourceFile, ActionLogger actionLogger)
    throws IOException, InvalidFormatException {
    Workbook template = WorkbookFactory.create(sourceFile);
    File file = dataDir.sourceExcelFile(resource, "data");
    //FileWriter csvFile = new FileWriter(file);
    //CSVWriter writer = new CSVWriter(csvFile, '\t', CSVWriter.NO_QUOTE_CHARACTER);
    OutputStream csvFile = new FileOutputStream(file);
    CSVWriter writer = new CSVWriter(new OutputStreamWriter(csvFile, "UTF-8"), '\t', CSVWriter.NO_QUOTE_CHARACTER);

    // Map of required columns
    Map<Integer, String> columnLocationRequiredElements = new HashMap<Integer, String>();

    // Read first row columns names
    Sheet sheet = template.getSheet("Plantilla");
    int totalColumns = sheet.getRow(0).getLastCellNum();
    int columnsWithRequiredElements = 0;
    Iterator<Row> rowIterator = sheet.rowIterator();
    String[] entries = new String[totalColumns];
    Row row = sheet.getRow(0);
    for (int counter = 0; counter < totalColumns; counter++) {
      if (!readCellValue(row.getCell(counter, Row.CREATE_NULL_AS_BLANK)).isEmpty()) {
        if (columnCoreTaxonomicMapping.containsKey(readCellValue(row.getCell(counter, Row.CREATE_NULL_AS_BLANK)))) {
          entries[counter] =
            columnCoreTaxonomicMapping.get(readCellValue(row.getCell(counter, Row.CREATE_NULL_AS_BLANK))).get(0);
          if (columnCoreTaxonomicMapping.get(readCellValue(row.getCell(counter, Row.CREATE_NULL_AS_BLANK))).get(1)
            .equalsIgnoreCase("required")) {
            // This column of template couldn't be empty, so map it
            columnLocationRequiredElements.put(counter,
              columnCoreTaxonomicMapping.get(readCellValue(row.getCell(counter, Row.CREATE_NULL_AS_BLANK))).get(0));
            columnsWithRequiredElements++;
          }
        }
      } else {
        entries[counter] = "";
      }
    }
    if (columnsWithRequiredElements != Integer
      .valueOf(columnCoreTaxonomicMapping.get("Total required elements").get(0))) {
      throw new InvalidFormatException(baseAction.getText("sibsp.application.portal.error.with.template.elements.data"));
    }
    writer.writeNext(entries);

    while (rowIterator.hasNext()) {
      row = rowIterator.next();
      if (row.getRowNum() != 0 && row.getRowNum() != 1) {
        for (int counter = 0; counter < totalColumns; counter++) {
          if (!readCellValue(row.getCell(counter, Row.CREATE_NULL_AS_BLANK)).isEmpty()) {
            entries[counter] = readCellValue(row.getCell(counter, Row.CREATE_NULL_AS_BLANK));
          } else {
            if (columnLocationRequiredElements.containsKey(counter)) {
              throw new InvalidFormatException(baseAction.getTextWithDynamicArgs(
                "sibsp.application.portal.error.element.notempty", columnLocationRequiredElements.get(counter)));
            } else {
              entries[counter] = "";
            }
          }
        }
        writer.writeNext(entries);
      }
    }

    writer.close();
    return file;
  }

  /**
   * Initialize Map of colums to read from template
   * csvFile file to import from to fill all template columns
   * 
   * @param csvFile
   * @return
   */
  private Map<String, ArrayList<String>> initializeColumnMapping(String csvFile) {
    Map<String, ArrayList<String>> columnMapping = new HashMap<String, ArrayList<String>>();
    int totalRequiredElements = 0;
    try {
      CSVReader reader =
        new CSVReader(new FileReader(dataDir.getDataDir() + "/" + DataDir.CONFIG_DIR + "/" + csvFile), ',', '"');
      String[] nextLine;
      ArrayList<String> list;
      while ((nextLine = reader.readNext()) != null) {
        list = new ArrayList<String>();
        list.add(nextLine[2]);
        list.add(nextLine[3]);
        columnMapping.put(nextLine[2], list);
        if (nextLine[3].equalsIgnoreCase("required")) {
          totalRequiredElements++;
        }
      }
      list = new ArrayList<String>();
      list.add(String.valueOf(totalRequiredElements));
      columnMapping.put("Total required elements", list);
      reader.close();
    } catch (FileNotFoundException exception) {
      exception.printStackTrace();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return columnMapping;
  }

  /**
   * Initialize Map of colums to read from template
   * csvFile1, csvFile2, csvFile3 files to import from to fill all template columns
   * 
   * @param csvFile1
   * @param csvFile2
   * @param csvFile3
   * @return
   */
  private Map<String, ArrayList<String>> initializeColumnMapping(String csvFile1, String csvFile2, String csvFile3) {
    Map<String, ArrayList<String>> columnMapping = new HashMap<String, ArrayList<String>>();
    int totalRequiredElements = 0;
    try {
      CSVReader reader =
        new CSVReader(new FileReader(dataDir.getDataDir() + "/" + DataDir.CONFIG_DIR + "/" + csvFile1), ',', '"');
      String[] nextLine;
      ArrayList<String> list;
      while ((nextLine = reader.readNext()) != null) {
        list = new ArrayList<String>();
        list.add(nextLine[2]);
        list.add(nextLine[3]);
        columnMapping.put(nextLine[2], list);
        if (nextLine[3].equalsIgnoreCase("required")) {
          totalRequiredElements++;
        }
      }
      reader =
        new CSVReader(new FileReader(dataDir.getDataDir() + "/" + DataDir.CONFIG_DIR + "/" + csvFile2), ',', '"');
      while ((nextLine = reader.readNext()) != null) {
        list = new ArrayList<String>();
        list.add(nextLine[2]);
        list.add(nextLine[3]);
        columnMapping.put(nextLine[2], list);
        if (nextLine[3].equalsIgnoreCase("required")) {
          totalRequiredElements++;
        }
      }
      reader =
        new CSVReader(new FileReader(dataDir.getDataDir() + "/" + DataDir.CONFIG_DIR + "/" + csvFile3), ',', '"');
      while ((nextLine = reader.readNext()) != null) {
        list = new ArrayList<String>();
        list.add(nextLine[2]);
        list.add(nextLine[3]);
        columnMapping.put(nextLine[2], list);
        if (nextLine[3].equalsIgnoreCase("required")) {
          totalRequiredElements++;
        }
      }

      list = new ArrayList<String>();
      list.add(String.valueOf(totalRequiredElements));
      columnMapping.put("Total required elements", list);
      reader.close();
    } catch (FileNotFoundException exception) {
      exception.printStackTrace();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return columnMapping;
  }

  private String readCellValue(Cell cell) throws InvalidFormatException {
    switch (cell.getCellType()) {
      case Cell.CELL_TYPE_STRING:
        return cell.getStringCellValue();
      case Cell.CELL_TYPE_NUMERIC:
        return Double.toString(cell.getNumericCellValue());
      case Cell.CELL_TYPE_BOOLEAN:
        return Boolean.toString(cell.getBooleanCellValue());
      case Cell.CELL_TYPE_FORMULA:
        return cell.getCellFormula();
      case Cell.CELL_TYPE_ERROR:
        throw new InvalidFormatException("Error en el formato de archivo");
      case Cell.CELL_TYPE_BLANK:
        return "";
      default:
        throw new InvalidFormatException("Error en el formato de archivo");
    }
  }
}
