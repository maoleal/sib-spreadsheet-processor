package net.sibcolombia.sibsp.action.portal;

import org.gbif.dwc.terms.ConceptTerm;
import org.gbif.ipt.task.Xls2Csv;
import org.gbif.ipt.utils.ActionLogger;
import org.gbif.ipt.utils.FileUtils;
import org.gbif.ipt.validation.ExtensionMappingValidator;
import org.gbif.ipt.validation.ExtensionMappingValidator.ValidationStatus;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import com.google.common.base.Strings;
import com.google.inject.Inject;

import net.sibcolombia.sibsp.configuration.ApplicationConfig;
import net.sibcolombia.sibsp.configuration.Constants;
import net.sibcolombia.sibsp.configuration.DataDir;
import net.sibcolombia.sibsp.model.Extension;
import net.sibcolombia.sibsp.model.ExtensionMapping;
import net.sibcolombia.sibsp.model.ExtensionProperty;
import net.sibcolombia.sibsp.model.PropertyMapping;
import net.sibcolombia.sibsp.model.RecordFilter;
import net.sibcolombia.sibsp.model.Resource.CoreRowType;
import net.sibcolombia.sibsp.model.Source;
import net.sibcolombia.sibsp.model.Source.FileSource;
import net.sibcolombia.sibsp.service.AlreadyExistingException;
import net.sibcolombia.sibsp.service.ImportException;
import net.sibcolombia.sibsp.service.InvalidFileExtension;
import net.sibcolombia.sibsp.service.InvalidFileName;
import net.sibcolombia.sibsp.service.admin.ExtensionManager;
import net.sibcolombia.sibsp.service.admin.VocabulariesManager;
import net.sibcolombia.sibsp.service.portal.ResourceManager;
import net.sibcolombia.sibsp.service.portal.SourceManager;
import net.sibcolombia.sibsp.struts2.SimpleTextProvider;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.exceptions.InvalidOperationException;

public class CreateResourceAction extends ManagerBaseAction {

	private static final long serialVersionUID = 3310022370019075108L;

	// logging
	private static final Logger log = Logger
			.getLogger(CreateResourceAction.class);

	private static final Pattern NORM_TERM = Pattern.compile("[\\W\\s_0-9]+");

	private final DataDir dataDir; // Directory to save temporal file

	// Data about file uploads
	private List<File> files;
	private List<String> fileContentTypes;
	private List<String> fileFileNames;
	private String shortname;
	private String onlyFileExtension;
	private final ExtensionManager extensionManager;
	private ExtensionMapping mapping;
	private final SourceManager sourceManager;
	private ExtensionProperty coreid;
	private PropertyMapping mappingCoreid;
	private final Xls2Csv excelToCsvConverter;
	
	private String emptySh;


	private List<String> columns;
	private List<PropertyMapping> fields;
	private List<String[]> peek;

	Boolean existMetadataFile = false;
	Boolean existAttributesFile = false;

	private final Map<String, Map<String, String>> vocabTerms = new HashMap<String, Map<String, String>>();
	private final VocabulariesManager vocabulariesManager;

	@Inject
	public CreateResourceAction(SimpleTextProvider textProvider,
			ApplicationConfig config, ResourceManager resourceManager,
			DataDir dataDir, ExtensionManager extensionManager,
			SourceManager sourceManager, Xls2Csv excelToCsvConverter,
			VocabulariesManager vocabulariesManager) {
		super(textProvider, config, resourceManager);
		this.extensionManager = extensionManager;
		this.dataDir = dataDir;
		this.sourceManager = sourceManager;
		this.excelToCsvConverter = excelToCsvConverter;
		this.vocabulariesManager = vocabulariesManager;
	}

	public void addWarnings() {
		if (mapping.getSource() == null) {
			return;
		}
		ExtensionMappingValidator validator = new ExtensionMappingValidator();
		ValidationStatus v = validator.validate(mapping, resource, peek);
		if (v != null && !v.isValid()) {
			if (v.getIdProblem() != null) {
				addActionWarning(getText(v.getIdProblem(), v.getIdProblemParams()));
			}
			for (ConceptTerm t : v.getMissingRequiredFields()) {
				addActionWarning(getText("validation.required",
						new String[] { t.simpleName() }));
			}
			for (ConceptTerm t : v.getWrongDataTypeFields()) {
				addActionWarning(getText("validation.wrong.datatype",
						new String[] { t.simpleName() }));
			}
		}
	}

	/**
	 * This method automaps a source's columns. First it tries to automap the
	 * mappingCoreId column, and then it tries to automap the source's remaining
	 * fields against the core/extension.
	 * 
	 * @return the number of terms that have been automapped
	 */
	int automap() {
		// keep track of how many terms were automapped
		int automapped = 0;

		// start by trying to automap the mappingCoreId (occurrenceId/taxonId) to a
		// column in source
		int idx1 = 0;
		for (String col : columns) {
			col = normalizeColumnName(col);
			if (col != null
					&& mappingCoreid.getTerm().simpleNormalisedName()
					.equalsIgnoreCase(col)) {
				// mappingCoreId and mapping id column must both be set (and have the
				// same index) to automap successfully.
				mappingCoreid.setIndex(idx1);
				mapping.setIdColumn(idx1);
				// we have automapped the core id column, so increment automapped
				// counter and exit
				automapped++;
				break;
			}
			idx1++;
		}

		// next, try to automap the source's remaining columns against the
		// extensions fields
		for (PropertyMapping f : fields) {
			int idx2 = 0;
			for (String col : columns) {
				col = normalizeColumnName(col);
				// log.info("Property: " + f.getTerm().simpleNormalisedName());
				if (col != null
						&& f.getTerm().simpleNormalisedName().equalsIgnoreCase(col)) {
					f.setIndex(idx2);
					// we have automapped the term, so increment automapped counter and
					// exit
					automapped++;
					break;
				}
				idx2++;
			}
		}

		return automapped;
	}

	private void explodeFileExtension(int position) {
		String fileName = fileFileNames.get(position);
		int dotPosition = fileName.lastIndexOf('.');
		if (dotPosition > 0 && dotPosition < fileName.length() - 1) {
			onlyFileExtension = fileName.substring(dotPosition + 1).toLowerCase();
		}
	}

	/**
	 * Calculate the size of the DwC-A file.
	 * 
	 * @return the size (human readable) of the DwC-A file.
	 */
	public String getDwcaFormattedSize() {
		return FileUtils.formatSize(resourceManager.getDwcaSize(resource), 2);
	}

	/**
	 * Calculate the size of the EML file.
	 * 
	 * @return the size (human readable) of the EML file.
	 */
	public String getEmlFormattedSize() {
		return FileUtils.formatSize(resourceManager.getEmlSize(resource), 2);
	}

	/**
	 * Calculate the size of the RTF file.
	 * 
	 * @return return the size (human readable) of the RTF file.
	 */
	public String getRtfFormattedSize() {
		return FileUtils.formatSize(resourceManager.getRtfSize(resource), 2);
	}

	public String getShortname() {
		return shortname;
	}

	/**
	 * Normalizes an incoming column name so that it can later be compared against
	 * a ConceptTerm's simpleNormalizedName. This method converts the incoming
	 * string to lower case, and will take the substring up to, but no including
	 * the first ":".
	 * 
	 * @param col
	 *          column name
	 * @return the normalized column name, or null if the incoming name was null
	 *         or empty
	 */
	String normalizeColumnName(String col) {
		if (!Strings.isNullOrEmpty(col)) {
			col = NORM_TERM.matcher(col.toLowerCase()).replaceAll("");
			if (col.contains(":")) {
				col = StringUtils.substringAfter(col, ":");
			}
			return col;
		}
		return null;
	}

	private void readSource() {
		if (mapping.getSource() == null) {
			columns = new ArrayList<String>();
		} else {
			peek = sourceManager.peek(mapping.getSource(), 5);
			// If user wants to import a source without a header lines, the columns
			// are going to be numbered with the first
			// non-null value as an example. Otherwise, read the file/database
			// normally.
			if (mapping.getSource().isFileSource()
					&& ((FileSource) mapping.getSource()).getIgnoreHeaderLines() == 0) {
				columns = mapping.getCompleteElementWithoutColumn(peek);
				log.info("Columna 1: " + columns.get(0));
				log.info("Columna 2: " + columns.get(1));
				log.info("Columna 3: " + columns.get(2));
			} else {
				columns = sourceManager.columns(mapping.getSource());
			}
		}
	}

	@Override
	public String save() throws IOException {
		ActionLogger actionLogger = new ActionLogger(this.log, this);
		try {
			Map<String, FileData> tmpFiles = uploadToTmp();
			File dataFileElements = null;
			if (tmpFiles != null) {
				Source source = null;
				Extension extension = new Extension();
				log.info("Processing attributes files");
				UUID uniqueID = UUID.randomUUID();
				this.resource = resourceManager.processMetadataSpreadsheetPart("tempMetadata", actionLogger);
				this.resource.setUniqueID(uniqueID);
				if (tmpFiles.get("ExcelFileComplete") != null) {
					log.info("Processing darwinCore attribute files");
					dataFileElements = excelToCsvConverter.convertExcelCoreCompleteToCsv(resource, tmpFiles.get("ExcelFileComplete").getFile(), actionLogger);
					source = sourceManager.add(this.resource, dataFileElements, tmpFiles.get("ExcelFileComplete").getFileName());
					this.resource.addSource(source, true);
					saveResource();
					extension = extensionManager.get(Constants.DWC_ROWTYPE_OCCURRENCE);
				} else {
					throw new ImportException("The sib processor application found an invalid file error.");
				}
				if (extension != null) {
					mapping = new ExtensionMapping();
					mapping.setExtension(extension);
				}
				if (mapping != null || mapping.getExtension() != null) {
					if (mapping.getSource() == null) {
						mapping.setSource(source);
					}
					// set empty filter if not existing
					if (mapping.getFilter() == null) {
						mapping.setFilter(new RecordFilter());
					}
					// determine the core row type
					String coreRowType = resource.getCoreRowType();
					if (coreRowType == null) {
						// not yet set, the current mapping must be the core type
						coreRowType = mapping.getExtension().getRowType();
					}
					// setup the core record id term
					String coreIdTerm = Constants.DWC_OCCURRENCE_ID;
					if (coreRowType.equalsIgnoreCase(Constants.DWC_ROWTYPE_TAXON)) {
						coreIdTerm = Constants.DWC_TAXON_ID;
					}
					coreid = extensionManager.get(coreRowType).getProperty(coreIdTerm);
					mappingCoreid = mapping.getField(coreid.getQualname());
					if (mappingCoreid == null) {
						// no, create bare mapping field
						mappingCoreid = new PropertyMapping();
						mappingCoreid.setTerm(coreid);
						mappingCoreid.setIndex(mapping.getIdColumn());
					}

					readSource();

					// prepare all other fields
					fields = new ArrayList<PropertyMapping>(mapping.getExtension()
							.getProperties().size());
					for (ExtensionProperty p : mapping.getExtension().getProperties()) {
						// ignore core id term
						if (p.equals(coreid)) {
							continue;
						}
						// uses a vocabulary?
						if (p.getVocabulary() != null) {
							vocabTerms.put(p.getVocabulary().getUriString(), vocabulariesManager.getI18nVocab(p.getVocabulary().getUriString(), getLocaleLanguage(), true));
						}
						// mapped already?
						PropertyMapping f = mapping.getField(p.getQualname());
						if (f == null) {
							// no, create bare mapping field
							f = new PropertyMapping();
						}
						f.setTerm(p);
						fields.add(f);
					}

					// finally do automap if no fields are found
					if (mapping.getFields().isEmpty()) {
						automap();
					}

					this.resource.addMapping(mapping);

					saveMapping();

					// remove all DwC mappings with 0 terms mapped
					for (ExtensionMapping em : resource.getCoreMappings()) {
						if (em.getFields().isEmpty()) {
							resource.deleteMapping(em);
						}
					}
				}
				if (resourceManager.publish(this.resource, this)) {
					addActionMessage(getText(
							"sibsp.application.portal.overview.publishing.resource.version",
							new String[] { Integer.toString(resource.getEmlVersion()) }));
				}
				// resourceManager.create(tmpFile, fileFileName, onlyFileName,
				// onlyFileExtension, this);
				log.info("File uploaded");
				if (tmpFiles.get("ExcelFileComplete") != null) {
					tmpFiles.get("ExcelFileComplete").getFile().delete();
				}
			} else {
				// addFieldError("file", "Debe subir la plantilla.");
				addActionError("Debe subir la plantilla.");
				return INPUT;
			}
		} catch (InvalidFileExtension error) {
			log.error("Spreadsheet template file extension is invalid.");
			addActionError(getText("sibsp.application.error.invalidextension"));
			return INPUT;
		} catch (InvalidFileName error) {
			log.error("Spreadsheet template file name is invalid.");
			addActionError(getText("sibsp.application.error.invalidfilename"));
			return INPUT;
		} catch (InvalidFormatException error) {
			log.error("Spreadsheet template file format error.");
			if (error.getMessage().isEmpty()) {
				addActionError(getText("sibsp.application.error.invalidfiletype"));
			} else {
				addFieldError("column", error.getMessage());
			}
			return INPUT;
		} catch (InvalidOperationException error) {
			log.error("Spreadsheet template file format error.");
			if (error.getMessage().isEmpty()) {
				addFieldError("other",
						getText("sibsp.application.error.invalidfiletype"));
			} else {
				String[] errors = error.getMessage().split("\n");
				for (int i = 0; i < errors.length; i++) {
					addFieldError(i + "", errors[i]);
				}
			}
			return INPUT;
		} catch (ImportException e) {
			log.error("File import error.");
			// e.printStackTrace(System.out);
			// addFieldError("file",
			// getText("sibsp.application.error.importexception"));
			addActionError(getText("sibsp.application.error.importexception"));
			return INPUT;
		} catch(EncryptedDocumentException error){
			log.error("The supplied spreadsheet seems to be an Encrypted .xlsx file. It must be decrypted before use by XSSF, it cannot be used by HSSF.");
			addActionError(getText("sibsp.application.error.encrypted"));
			return INPUT;
		} catch (AlreadyExistingException e) {
			log.error("File already exist.");
			return INPUT;
		}
		
		
		
		if(resource.getRecordsPublished()==0){
			log.error("The file is empty, no records");
			addActionError(getText("sibsp.application.error.empty"));
			this.emptySh ="Libro vacio";
			return INPUT;
		}else{
			return SUCCESS;
		}
		
		
	}

	public String saveMapping() throws IOException {
		// save field mappings
		Set<PropertyMapping> mappedFields = new HashSet<PropertyMapping>();
		for (PropertyMapping f : fields) {
			if (f.getIndex() != null
					|| StringUtils.trimToNull(f.getDefaultValue()) != null) {
				mappedFields.add(f);
			}
		}
		// save coreid field
		mappingCoreid.setIndex(mapping.getIdColumn());
		mappingCoreid.setDefaultValue(mapping.getIdSuffix());
		if (mappingCoreid.getIndex() != null
				|| StringUtils.trimToNull(mappingCoreid.getDefaultValue()) != null) {
			mappedFields.add(mappingCoreid);
		}
		// back to mapping object
		mapping.setFields(mappedFields);

		// update core type
		updateResourceCoreType(mapping, mappedFields.size());

		// set modified date
		resource.setModified(new Date());
		// save entire resource config
		saveResource();
		// report validation without skipping this save
		addWarnings();

		return defaultResult;
	}

	public void setFile(List<File> files) {
		this.files = files;
	}

	public void setFileContentType(List<String> fileContentTypes) {
		this.fileContentTypes = fileContentTypes;
	}

	public void setFileFileName(List<String> fileFileNames) {
		this.fileFileNames = fileFileNames;
	}

	public void setShortname(String shortname) {
		this.shortname = shortname;
	}

	/**
	 * Update resource core type. This must be done every time the resource's core
	 * type mapping is being modified, or deleted. If it is the 1st mapping of the
	 * core type, the core type won't have been set yet. Only if 1 or more mapped
	 * fields were saved, can we consider the mapping to have been legitimate.
	 * Furthermore, if the core type mapping is being deleted, then the resource
	 * must reset its core type to null.
	 * 
	 * @param mapping
	 *          ExtensionMapping
	 * @param mappedFields
	 *          the number of mapped fields in the mapping - set to 0 if the
	 *          mapping is to be deleted
	 */
	void updateResourceCoreType(ExtensionMapping mapping, int mappedFields) {
		// proceed only if we're dealing with the core type mapping
		if (mapping.isCore()) {
			// must be 1 or more mapped fields for mapping to be legitimate
			if (mappedFields > 0) {
				resource.setCoreType(resource.getCoreRowType().equalsIgnoreCase(
						Constants.DWC_ROWTYPE_TAXON) ? StringUtils
								.capitalize(CoreRowType.CHECKLIST.toString()) : StringUtils
								.capitalize(CoreRowType.OCCURRENCE.toString()));
			}
			// otherwise, reset core type!
			else {
				resource.setCoreType(null);
			}
		}
	}

	private Map<String, FileData> uploadToTmp() throws InvalidFileExtension, InvalidFileName {
		if (fileFileNames == null) {
			return null;
		}
		Map<String, FileData> tmpFiles = new HashMap<String, FileData>();
		for (int i = 0; i < fileFileNames.size(); i++) {
			explodeFileExtension(i);
			if (validExtension()) {
				FileData fileData = new FileData(fileFileNames.get(i), dataDir.tmpFile(
						"temp", fileFileNames.get(i)));
				tmpFiles.put("ExcelFileComplete", fileData);
				InputStream input = null;
				OutputStream output = null;
				try {
					input = new FileInputStream(files.get(i));
					// write the file to the file specified
					output = new FileOutputStream(fileData.getFile());
					IOUtils.copy(input, output);
					output.flush();
					log.debug("Uploaded file " + fileFileNames.get(i)
							+ " with content-type " + fileContentTypes.get(i));
				} catch (IOException e) {
					log.error(e);
					return null;
				} finally {
					if (output != null) {
						IOUtils.closeQuietly(output);
					}
					if (input != null) {
						IOUtils.closeQuietly(input);
					}
				}
			} else {
				throw new InvalidFileExtension("invalid file extension");
			}
		}
		return tmpFiles;
	}

	/**
	 * Check if the source file is a valid spreadsheet template file extension
	 * 
	 * @return
	 */
	private boolean validExtension() {
		if (onlyFileExtension == null) {
			return false;
		} else {
			if (onlyFileExtension.equalsIgnoreCase("xls")
					|| onlyFileExtension.equalsIgnoreCase("xlsx")) {
				return true;
			} else {
				return false;
			}
		}
	}

	private class FileData {
		private String fileName;
		private File file;

		public FileData(String fileName, File file) {
			this.file = file;
			this.fileName = fileName;
		}

		public String getFileName() {
			return fileName;
		}

		public File getFile() {
			return file;
		}

	}
	
	public String getEmptySh() {
		return emptySh;
	}

	public void setEmptySh(String emptySh) {
		this.emptySh = emptySh;
	}

}
