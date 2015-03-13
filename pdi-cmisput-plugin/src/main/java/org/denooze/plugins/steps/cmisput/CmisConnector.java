 /* Copyright (c) 2011 Gunter Rombauts.  All rights reserved. 
 * This software was developed by Gunter Rombauts and is provided under the terms 
 * of the GNU Lesser General Public License, Version 2.1. You may not use 
 * this file except in compliance with the license. If you need a copy of the license, 
 * please go to http://www.gnu.org/licenses/lgpl-2.1.txt. The Original Code is cmisput.  
 * The Initial Developer is Gunter Rombauts.
 *
 * Software distributed under the GNU Lesser Public License is distributed on an "AS IS" 
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or  implied. Please refer to 
 * the license for the specific language governing your rights and limitations.*/

package org.denooze.plugins.steps.cmisput;

import java.io.File;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
//import org.apache.tika.metadata.Metadata;
//import org.apache.tika.parser.AutoDetectParser;
//import org.apache.tika.parser.Parser;
//import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.ContentHandler;

import eu.medsea.mimeutil.MimeType;
import eu.medsea.mimeutil.MimeUtil;

//import org.alfresco.cmis.client.AlfrescoDocument;
//import org.alfresco.cmis.client.AlfrescoFolder;
import org.alfresco.cmis.client.AlfrescoDocument;
import org.alfresco.cmis.client.AlfrescoFolder;
import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.FileableCmisObject;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.ItemIterable;
import org.apache.chemistry.opencmis.client.api.ObjectId;
import org.apache.chemistry.opencmis.client.api.ObjectType;
import org.apache.chemistry.opencmis.client.api.QueryResult;
import org.apache.chemistry.opencmis.client.api.Repository;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.client.api.SessionFactory;
import org.apache.chemistry.opencmis.client.api.Tree;
import org.apache.chemistry.opencmis.client.bindings.CmisBindingFactory;
import org.apache.chemistry.opencmis.client.runtime.SessionFactoryImpl;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.data.CmisExtensionElement;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.data.RepositoryInfo;
import org.apache.chemistry.opencmis.commons.definitions.PropertyDefinition;
import org.apache.chemistry.opencmis.commons.enums.BindingType;
import org.apache.chemistry.opencmis.commons.enums.Updatability;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.chemistry.opencmis.commons.exceptions.CmisBaseException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisConnectionException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ContentStreamImpl;
import org.apache.chemistry.opencmis.commons.spi.CmisBinding;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ChoiceImpl;
import org.apache.commons.vfs.FileObject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleFileException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.vfs.KettleVFS;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.steps.pgbulkloader.PGBulkLoaderMeta;
import org.pentaho.di.ui.core.dialog.ErrorDialog;
import org.pentaho.di.ui.core.widget.TableView;
import org.pentaho.di.ui.core.widget.TextVar;



/**
 * @author Gunter
 *
 */
public class CmisConnector implements Cloneable
{

	private static Class<?> PKG = CmisPutMeta.class; // for i18n purposes, needed by Translator2!!   $NON-NLS-1$
	private Properties cmsProperties;
    private Session session;
    private Iterable<Repository> repolist;
    private String msgError;
    private String localNameSpaceFilter;
    private AlfrescoFolder parentFolder;
    private AlfrescoDocument cmsdocument;
    private String lastCreatedDynPath;
    private String cmisProductName;
    private String cmisVendorName;
    private String cmisVersionSupported;
    private VersioningState VersioningState;
    private Map<String, Object> documentproperties = new HashMap<String, Object>();
    private String separatorChar = ";";
    /**
	 * 
	 */
    public CmisConnector(final Properties properties) {
	this.cmsProperties = properties;
	this.session =  null;
	this.setRepolist(null);
//	this.session = initCmisSession();
    }

	/* initialize CMIS session */
	public void initCmisSession() {

		Session sess;
		setMsgError(null);
			try {
				// Default factory implementation of client runtime.
				final SessionFactory sessionFactory = SessionFactoryImpl.newInstance();
				final Map<String, String> parameter = new HashMap<String, String>();

				parameter.put(SessionParameter.USER, this.cmsProperties.getProperty("cms.username"));
				parameter.put(SessionParameter.PASSWORD, this.cmsProperties.getProperty("cms.password"));
				parameter.put(SessionParameter.ATOMPUB_URL, this.cmsProperties.getProperty("cms.url"));
				parameter.put(SessionParameter.BINDING_TYPE, BindingType.ATOMPUB.value());
				parameter.put(SessionParameter.REPOSITORY_ID, this.cmsProperties.getProperty("cms.repoId"));
				parameter.put(SessionParameter.OBJECT_FACTORY_CLASS, "org.alfresco.cmis.client.impl.AlfrescoObjectFactoryImpl");
//				parameter.put(SessionParameter.CLIENT_COMPRESSION, "true");

			    CmisBindingFactory factory = CmisBindingFactory.newInstance();
			    CmisBinding binding = factory.createCmisAtomPubBinding(parameter);
				RepositoryInfo repositoryInfo = binding.getRepositoryService().getRepositoryInfo(this.cmsProperties.getProperty("cms.repoId") , null);
			    
				//get information about the system
				setCmisProductName(repositoryInfo.getProductName());
				setCmisVendorName(repositoryInfo.getVendorName());
				setCmisVersionSupported(repositoryInfo.getCmisVersionSupported());

				sess = sessionFactory.createSession(parameter);

				// Turn off the session cache completely
				sess.getDefaultContext().setCacheEnabled(false);
				
				
			} catch (Exception e) {
				setMsgError(e.getMessage());
				sess = null;
			}			
			this.session = sess;
	    }
	/* Get list of repositories */
	public void getRepositories(CCombo wRepository) {

		Iterable<Repository> repolist;
		setMsgError(null);
			try {
				// Default factory implementation of client runtime.
				final SessionFactory sessionFactory = SessionFactoryImpl.newInstance();
				final Map<String, String> parameter = new HashMap<String, String>();

				parameter.put(SessionParameter.USER, this.cmsProperties.getProperty("cms.username"));
				parameter.put(SessionParameter.PASSWORD, this.cmsProperties.getProperty("cms.password"));
				parameter.put(SessionParameter.ATOMPUB_URL, this.cmsProperties.getProperty("cms.url"));
				parameter.put(SessionParameter.BINDING_TYPE, BindingType.ATOMPUB.value());
				parameter.put(SessionParameter.OBJECT_FACTORY_CLASS, "org.alfresco.cmis.client.impl.AlfrescoObjectFactoryImpl");

				repolist = sessionFactory.getRepositories(parameter);	
				for (Repository repoID : repolist) {
					wRepository.add(repoID.getId());
				}
								
			} catch (Exception e) {
				setMsgError(e.getMessage());
				repolist = null;
			}			
			this.setRepolist(repolist);
	    }
	/**
	 * @return the cmsProperties
	 */
	public Properties getCmsProperties() {
		return cmsProperties;
	}

	/**
	 * @param cmsProperties the cmsProperties to set
	 */
	public void setCmsProperties(Properties cmsProperties) {
		this.cmsProperties = cmsProperties;
	}

	/**
	 * @return the session
	 */
	 public Session getSession() {
		 return this.session;
	    }
	 /**
	 * @return the session
	 */
	 public void clearSession() {
		 this.session.clear();
	    }
	/**
	 * @return the repolist
	 */
	public Iterable<Repository> getRepolist() {
		return repolist;
	}

	/**
	 * @param repolist the repolist to set
	 */
	public void setRepolist(Iterable<Repository> repolist) {
		this.repolist = repolist;
	}

	/**
	 * @return the msgError
	 */
	public String getMsgError() {
		return msgError;
	}

	/**
	 * @param msgError the msgError to set
	 */
	public void setMsgError(String msgError) {
		this.msgError = msgError;
	}
	/**
	 * @return the localNameSpaceFilter
	 */
	public String getLocalNameSpaceFilter() {
		return localNameSpaceFilter;
	}

	/**
	 * @param localNameSpaceFilter the localNameSpaceFilter to set
	 */
	public void setLocalNameSpaceFilter(String localNameSpaceFilter) {
		this.localNameSpaceFilter = localNameSpaceFilter;
	}

	/**
	 * @return the cmisProductName
	 */
	public String getCmisProductName() {
		return cmisProductName;
	}

	/**
	 * @param cmisProductName the cmisProductName to set
	 */
	public void setCmisProductName(String cmisProductName) {
		this.cmisProductName = cmisProductName;
	}

	/**
	 * @return the cmisVendorName
	 */
	public String getCmisVendorName() {
		return cmisVendorName;
	}

	/**
	 * @param cmisVendorName the cmisVendorName to set
	 */
	public void setCmisVendorName(String cmisVendorName) {
		this.cmisVendorName = cmisVendorName;
	}

	/**
	 * @return the cmisVersionSupported
	 */
	public String getCmisVersionSupported() {
		return cmisVersionSupported;
	}

	/**
	 * @param cmisVersionSupported the cmisVersionSupported to set
	 */
	public void setCmisVersionSupported(String cmisVersionSupported) {
		this.cmisVersionSupported = cmisVersionSupported;
	}

	/**
	 * @return the versioningState
	 */
	public VersioningState getVersioningState() {
		return VersioningState;
	}

	/**
	 * @param versioningState the versioningState to set
	 */
	public void setVersioningState(VersioningState versioningState) {
		VersioningState = versioningState;
	}

	/**
	 * @return the documentproperties
	 */
	public Map<String, Object> getDocumentproperties() {
		return documentproperties;
	}
	
	public static int safeLongToInt(long l) {
		if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
		throw new IllegalArgumentException (l + " cannot be cast to int without changing its value.");
		}
		return (int) l; 
	} 

	/**
	 * @param documentproperties the documentproperties to set
	 */
	public boolean setDocumentproperty(CmisPutMeta meta, RowMetaInterface rowMeta, Integer index, Object value, Integer rowIndex) {
		Boolean result = false;
		String dataType = meta.getDocumentPropertyDataType()[index]; 
		String key = meta.getDocumentPropertyName()[index];
		String cardinality = meta.getDocumentPropertyCardinality()[index];
		
		ValueMetaInterface valueMeta = rowMeta.getValueMeta(rowIndex);
	
		switch(valueMeta.getType()) {
		case ValueMetaInterface.TYPE_STRING :
			if (cardinality.contains("SINGLE")) {
				if (dataType.contains("STRING")) {
					this.documentproperties.put(key, value);
					result = true;
				}
			} else { /* Cardinality is MULTI */
				if (dataType.contains("STRING")) {
					ArrayList<String> al = new ArrayList<String>();
					String multiString;
					
					if (value instanceof String) {
						multiString = (String) value;
					} else {
						multiString = value.toString();
					}
					String[] multiStringParts = multiString.split(separatorChar);
					for (int i=0;i<multiStringParts.length;i++) {
						al.add(multiStringParts[i]);
					}
					this.documentproperties.put(key, al);
					result = true;
				}
				if (dataType.contains("INTEGER")) {
					ArrayList<Integer> al = new ArrayList<Integer>();
					String multiString;
					multiString = (String) value;
					
					String[] multiStringParts = multiString.split(separatorChar);
					for (int i=0;i<multiStringParts.length;i++) {
						try {
							al.add(Integer.parseInt(multiStringParts[i]));
						} 
						catch (NumberFormatException e) {
							setMsgError(e.getMessage());
							return false;
						}
					}
					this.documentproperties.put(key, al);
					result = true;
				}
				if (dataType.contains("BOOLEAN")) {
					ArrayList<Boolean> al = new ArrayList<Boolean>();
					String multiString;
					multiString = (String) value;
					
					String[] multiStringParts = multiString.split(separatorChar);
					for (int i=0;i<multiStringParts.length;i++) {
						try {
							if (multiStringParts[i].equals("Y")){
								al.add(true);
							} else {
								al.add(false);
							}
						} 
						catch (NumberFormatException e) {
							setMsgError(e.getMessage());
							return false;
						}
					}
					this.documentproperties.put(key, al);
					result = true;
				}
				if (dataType.contains("DATETIME")) {
					ArrayList<GregorianCalendar> al = new ArrayList<GregorianCalendar>();
					String multiString;
					multiString = (String) value;
					
					String[] multiStringParts = multiString.split(separatorChar);
					for (int i=0;i<multiStringParts.length;i++) {
						try {
							GregorianCalendar cal = new GregorianCalendar();
							if (multiStringParts[i].length() > 10){
								DateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
								cal.setTime(formatter.parse(multiStringParts[i]));
								al.add(cal);
							} else {
								DateFormat formatter = new SimpleDateFormat("yyyy/MM/dd");
								cal.setTime(formatter.parse(multiStringParts[i]));
								al.add(cal);
							}
						} 
						catch (ParseException e) {
							setMsgError(e.getMessage());
							return false;
						}
					}
					this.documentproperties.put(key, al);
					result = true;
				}
				if (dataType.contains("DECIMAL")) {
					ArrayList<Double> al = new ArrayList<Double>();
					String multiString;
					multiString = (String) value;
					
					String[] multiStringParts = multiString.split(separatorChar);
					for (int i=0;i<multiStringParts.length;i++) {
						try {
							al.add(Double.parseDouble(multiStringParts[i]));
						} 
						catch (NumberFormatException e) {
							setMsgError(e.getMessage());
							return false;
						}
					}
					this.documentproperties.put(key, al);
					result = true;
				}
			}
			break;
		case ValueMetaInterface.TYPE_INTEGER:
			if (cardinality.contains("SINGLE")) {
				if (dataType.contains("INTEGER")) {
					this.documentproperties.put(key, value);
					result = true;
				}
			} else { /* Cardinality is MULTI */
				if (dataType.contains("INTEGER")) {
					ArrayList<Integer> al = new ArrayList<Integer>();
					al.add((Integer) value);
					this.documentproperties.put(key, al);
					result = true;
				}
			}
			break;
		case ValueMetaInterface.TYPE_DATE:
			if (cardinality.contains("SINGLE")) {
				if (dataType.contains("DATETIME")) {
					GregorianCalendar cal = new GregorianCalendar();
					cal.setTime((Date) value);
					this.documentproperties.put(key, cal);
					result = true;
				}
			} else { /* Cardinality is MULTI */
				if (dataType.contains("DATETIME")) {
					ArrayList<GregorianCalendar> al = new ArrayList<GregorianCalendar>();
					GregorianCalendar cal = new GregorianCalendar();
					cal.setTime((Date) value);
					al.add(cal);
					this.documentproperties.put(key, al);
					result = true;
				}
			}
			break;
		case ValueMetaInterface.TYPE_BOOLEAN:
			if (cardinality.contains("SINGLE")) {
				if (dataType.contains("BOOLEAN")) {
					this.documentproperties.put(key, value);
					result = true;
				}
			} else { /* Cardinality is MULTI */
				if (dataType.contains("BOOLEAN")) {
					ArrayList<Boolean> al = new ArrayList<Boolean>();
					al.add((Boolean) value);
					this.documentproperties.put(key, al);
					result = true;
				}
			}
			break;
		case ValueMetaInterface.TYPE_NUMBER:
			if (cardinality.contains("SINGLE")) {
				if (dataType.contains("DECIMAL")) {
					this.documentproperties.put(key, value);
					result = true;
				}
			} else { /* Cardinality is MULTI */
				if (dataType.contains("DECIMAL")) {
					ArrayList<Double> al = new ArrayList<Double>();
					al.add((Double) value);
					this.documentproperties.put(key, al);
					result = true;
				}
			}
			break;
		case ValueMetaInterface.TYPE_BIGNUMBER:
			/* CMIS does not support this type*/
			break;
		}
		return result;
	}

	/**
	 * @param documentproperties the documentproperties to set
	 */
	public void removeDocumentproperties() {
		this.documentproperties.clear();
	}

	/**
	 * @return the parentFolder
	 */
	public AlfrescoFolder getParentFolder() {
		return parentFolder;
	}

	/**
	 * @param parentFolder the parentFolder to set
	 */
//	public void setParentFolder(AlfrescoFolder parentFolder) {
//		this.parentFolder = parentFolder;
//	}

	/**
	 * @return the lastCreatedDynPath
	 */
	public String getLastCreatedDynPath() {
		return lastCreatedDynPath;
	}

	/**
	 * @param lastCreatedDynPath the lastCreatedDynPath to set
	 */
	public void setLastCreatedDynPath(String lastCreatedDynPath) {
		this.lastCreatedDynPath = lastCreatedDynPath;
	}

	/**
	 * 
	 */
	public Document getDocumentByPath(String cmsfilename){
		Document Doc = null;
		String documentPath = getLastCreatedDynPath() + "/" + cmsfilename;
		try {
			Doc  = (Document) session.getObjectByPath(documentPath);
		} catch (CmisObjectNotFoundException e) {
			setMsgError(e.getMessage());
		}
		return Doc;
	}
	
	public ItemIterable<QueryResult> getDocumentByQuery(String query){
		query = "SELECT * FROM qp:quotationspace where cmis:name = 'Quotation Test 3'";
		ItemIterable<QueryResult> results = session.query(query, false);
		return results;
	}
	
	public void GetDocumentTypeList(CCombo wDocumentType, TextVar wBaseContentModel){
		ItemIterable<ObjectType> childDocType = session.getTypeChildren(wBaseContentModel.getText(), true);
		for (ObjectType objectType : childDocType) {
			String localNameSpace = objectType.getLocalNamespace();
			if ((localNameSpaceFilter!=null) && (!localNameSpaceFilter.isEmpty())){
				if (localNameSpace.contains(localNameSpaceFilter))
				{
//					wDocumentType.add(objectType.getDisplayName()+" ("+objectType.getId()+")");
					wDocumentType.add(objectType.getId());
					GetChildDocumentTypeList(objectType.getId(),wDocumentType);
				}
			} else {
//				wDocumentType.add(objectType.getDisplayName()+" ("+objectType.getId()+")");
				wDocumentType.add(objectType.getId());
				GetChildDocumentTypeList(objectType.getId(),wDocumentType);
			}
		}
	}

	private void GetChildDocumentTypeList(String parentDocumentType,CCombo wDocumentType){
		/*TODO - test it and integrate in ui*/
		ItemIterable<ObjectType> childDocType = session.getTypeChildren(parentDocumentType, true);
		for (ObjectType objectType : childDocType) {
			String localNameSpace = objectType.getLocalNamespace();
			if ((localNameSpaceFilter!=null) && (!localNameSpaceFilter.isEmpty())){
				if (localNameSpace.contains(localNameSpaceFilter))
				{
//					wDocumentType.add(objectType.getDisplayName()+" ("+objectType.getId()+")");
					wDocumentType.add(objectType.getId());
					GetChildDocumentTypeList(objectType.getId(),wDocumentType);
				}
			} else {
//				wDocumentType.add(objectType.getDisplayName()+" ("+objectType.getId()+")");
				Map<String, PropertyDefinition<?>> propertyDefinitions = objectType.getPropertyDefinitions();
				
				wDocumentType.add(objectType.getId());
				GetChildDocumentTypeList(objectType.getId(),wDocumentType);
			}
		}
	}

	public void GetFolderTypeList(CCombo wDocumentType, String cmisBaseFolderType){
		ItemIterable<ObjectType> childDocType = session.getTypeChildren(cmisBaseFolderType, true);
		wDocumentType.add(cmisBaseFolderType);
		for (ObjectType objectType : childDocType) {
			String localNameSpace = objectType.getLocalNamespace();
			if ((localNameSpaceFilter!=null) && (!localNameSpaceFilter.isEmpty())){
				if (localNameSpace.contains(localNameSpaceFilter))
				{
					wDocumentType.add(objectType.getId());
					GetFolderTypeList(wDocumentType,objectType.getId());
				}
			} else {
				wDocumentType.add(objectType.getId());
				GetFolderTypeList(wDocumentType,objectType.getId());
			}
		}
	}
	
	public Map<String, Integer> GetAspectList(){

		final Map<String, Integer> fields = new HashMap<String, Integer>();
		int i = 0;
		
		ItemIterable<ObjectType> aspects = session.getTypeChildren("cmis:policy", false);
		for (ObjectType objectType : aspects) {
			String localNameSpace = objectType.getLocalNamespace();
			if ((localNameSpaceFilter!=null) && (!localNameSpaceFilter.isEmpty())){
				if (localNameSpace.contains(localNameSpaceFilter)) {
					fields.put(objectType.getId(),i);
				}
			} else {
				fields.put(objectType.getId(),i);
			}	
			i+=1;
		}
		return fields;
	}
	
	public void CheckSession(){
		if (getSession()== null) {				
			initCmisSession();
		}
	}

	public Boolean CreateDynPathIfNotExists(String topath, Object[] r, int[] folderArgumentIndexes, String[] object_type_id) {

		final Map<String, Object> properties = new HashMap<String, Object>();
		String CurrentFolder;
		String FolderContent;

		if (topath == null) {
			topath = "/";
		}

		if (topath.endsWith("/")) {
			topath = topath.substring(0, topath.length() - 1);
		}

		CheckSession();
		parentFolder = (AlfrescoFolder) getSession().getObjectByPath(topath);

		for (int i = 0; i < folderArgumentIndexes.length; i++) {
			FolderContent = (String) r[folderArgumentIndexes[i]];
			if (FolderContent != null) {/* we ignore empty folders */
				// topath = topath + "/" + CurrentFolder.trim();
				// topath.replace("//", "/");
				// StringTokenizer tokenizer = new StringTokenizer(topath, "/");
				//
				// AlfrescoFolder Folder = null;
				// try {
				// Folder = (AlfrescoFolder)
				// getSession().getObjectByPath(topath);
				// } catch (CmisObjectNotFoundException e) {
				// try {
				// Folder =
				// createFolder(parentFolder,CurrentFolder,properties,object_type_id[i]);
				// } catch (Exception e1) {
				// setMsgError(e.getMessage());
				// return false;
				// }
				// }
				// parentFolder = Folder;
				// }
				// setLastCreatedDynPath(topath);

				StringTokenizer tokenizer = new StringTokenizer(FolderContent,
						"/");
				while (tokenizer.hasMoreTokens()) {
					CurrentFolder = tokenizer.nextToken();
					if (topath != null) {
						topath = topath + "/" + CurrentFolder.trim();
					} else {
						topath = "/" + CurrentFolder;
					}
					AlfrescoFolder Folder = null;
					Boolean Retry = true;
					Boolean First = true;
					Integer RetryCount = 0;/*
											 * If at the same time 2 copies of a
											 * step try to create a directory,
											 * do a retry
											 */
					while ((RetryCount < 100) && (Retry)) {
						RetryCount += 1;
						try {
							Folder = (AlfrescoFolder) getSession()
									.getObjectByPath(topath);
							Retry = false;
						} catch (CmisObjectNotFoundException e) {
							try {
								if (First) {
									//DRH 
									//randomDelay(100, 2000);
									randomDelay(10, 100);
									First = false;
								} else {
									Folder = createFolder(parentFolder,	CurrentFolder, properties, object_type_id[i]);
									Retry = false;
								}
							} catch (Exception e1) {
								setMsgError("Retry count = " + RetryCount
										+ " Message = " + e.getMessage());
							}
						} catch (CmisBaseException e) {
							setMsgError("Retry count = " + RetryCount
									+ " Message = " + e.getMessage());
							
							//DRH 
							//randomDelay(100, 2000);
							randomDelay(20, 500);
						} finally {
							if (RetryCount == 99)
								return false;
						}
					}
					parentFolder = Folder;
					setLastCreatedDynPath(topath);
				}
			}
		}
		return true;
	}
	
	public Boolean CreatePathIfNotExists(String topath, String object_type_id){
	    final Map<String, Object> properties = new HashMap<String, Object>();
		parentFolder = (AlfrescoFolder) getSession().getRootFolder();
		if (topath==null) {
			return false;
		}
		topath.replace("\"", "/");
		StringTokenizer tokenizer = new StringTokenizer(topath, "/"); 
		String CurrentFolder;
		
		topath = null;
		while (tokenizer.hasMoreTokens()){
			CurrentFolder = tokenizer.nextToken();
			if (topath != null) {
				topath = topath + "/" + CurrentFolder.trim();
			} else {
				topath = "/" + CurrentFolder;
			}
			AlfrescoFolder Folder = null;
			Boolean Retry= true; 
			Boolean First= true; 
			Integer RetryCount = 0;/*If at the same time 2 copies of a step try to create a directory, do a retry */
			while ((RetryCount <50) && (Retry)) {
				RetryCount+=1;
				try {
					Folder = (AlfrescoFolder) getSession().getObjectByPath(topath);
					Retry = false;
				} catch (CmisObjectNotFoundException e) {//folder does not exists
					try {
						if (First){//wait random time before creating the folder.
							randomDelay(100,2000);
							First=false;
						} else {
							Folder = createFolder(parentFolder,CurrentFolder,properties,object_type_id);
							Retry = false;
						}
					} catch (Exception e1) {
						setMsgError("Retry count = "+RetryCount+" Message = "+e.getMessage());
					}
				} catch (CmisBaseException  e) {
					setMsgError("Retry count = "+RetryCount+" Message = "+e.getMessage());
					randomDelay(100,2000);
				} finally {
					if (RetryCount == 49) return false;
				}	
			}
					
			parentFolder = Folder;
			setLastCreatedDynPath(topath);
		}
		return true;
	}
	void randomDelay(float min, float max){	
		int random = (int)(max * Math.random() + min);	
		try {		
			Thread.sleep(random);	
			} 
		catch (InterruptedException e) {
			// TODO Auto-generated catch block		e.printStackTrace();	
			}
	}

	public Boolean CreateDocument(String documenttype,String cmsfilename,String sourcefile, TransMeta transmeta){

	    final Map<String, Object> properties = getDocumentproperties();
		FileInputStream file;
		if (sourcefile==null){
			return false;
		}
		FileObject fileObject = null;
		try {
			fileObject = KettleVFS.getFileObject(sourcefile, transmeta);
		} catch (KettleFileException e) {
//			/* should never occur - as previous checks will reveal all error with this cause.*/
			setMsgError("KettleFileException " + e.getMessage());
		}
		try {
			file = new FileInputStream(KettleVFS.getFilename(fileObject));
		} catch (FileNotFoundException e) {
			setMsgError("FileNotFoundException "+e.getMessage());
			return false;
		}
		
		ContentStream content = new ContentStreamImpl(cmsfilename, null, DetectMimeType(sourcefile, transmeta), file);
	    properties.put(PropertyIds.OBJECT_TYPE_ID, documenttype);
	    setCmsdocument(createDocument(getParentFolder(),content,properties));
	    if (getCmsdocument()==null){
			return false;
	    } else {
			return true;
	    }
	}
	
	private void checkTypeSet(final Map<String, Object> properties) {
		if (!properties.containsKey(PropertyIds.OBJECT_TYPE_ID)) {
		    throw new Error("Object type id is not set.");
		}
	}
	
	public AlfrescoDocument createDocument(final Folder parent, final ContentStream content, final Map<String, Object> properties) {
		Document newDocument = null;
		properties.put(PropertyIds.NAME, content.getFileName());
		checkTypeSet(properties);
		
		try {
			newDocument = parent.createDocument(properties, content, getVersioningState());
		} catch (Exception e) {
			setMsgError("createDocument Exception "+e.getMessage());
		}
		return (AlfrescoDocument) newDocument;
	}
	
	public AlfrescoFolder createFolder(final Folder parent, final String name, final Map<String, Object> properties, String object_type_id) {
	    properties.put(PropertyIds.OBJECT_TYPE_ID, object_type_id);
		properties.put(PropertyIds.NAME, name);

		final Folder newFolder = parent.createFolder(properties);

		return (AlfrescoFolder) newFolder;
	}

	/**
	 * @return the cmsdocument
	 */
	public AlfrescoDocument getCmsdocument() {
		return cmsdocument;
	}

	/**
	 * @param cmsdocument the cmsdocument to set
	 */
	public void setCmsdocument(AlfrescoDocument cmsdocument) {
		this.cmsdocument = cmsdocument;
	}
	/**
	 * @return if the document is versionable
	 */
	public Boolean DocumentIsVersionable(Document CmisDoc) {
		/*TODO implement*/
		return true;
	}
	/**
	 * @return if the document is checked out
	 */
	public Boolean DocumentIsCheckOut(Document CmisDoc) {
		/*TODO implement check */
		if (CmisDoc.isVersionSeriesCheckedOut()) {
			return true;
		} else {
			return false;
		}
	}
	/**
	 * @return checked out document
	 */
	public ObjectId CheckOutDocument(Document CmisDoc) {
		ObjectId id = CmisDoc.checkOut();
		return id;
	}

	public void CancelCheckOutDocument(ObjectId CmisDocId){
		/*TODO implement*/
		AlfrescoDocument document = (AlfrescoDocument) session.getObject(CmisDocId);
		document.cancelCheckOut();
	}
	
	public Boolean CheckInDocument(ObjectId CmisDocId,String documenttype,String cmsfilename,String sourcefile,String comment, TransMeta transmeta) {

		ObjectId checkedInDocId; 

	    final Map<String, Object> properties = getDocumentproperties();
		FileInputStream file = null;
		
		if (sourcefile==null){
			return false;
		}
		
		FileObject fileObject = null;
		try {
			fileObject = KettleVFS.getFileObject(sourcefile, transmeta);
		} catch (KettleFileException e) {
//			/* should never occur - as previous checks will reveal all error with this cause.*/
			setMsgError(e.getMessage());
		}
		
		try {
			file = new FileInputStream(KettleVFS.getFilename(fileObject));
			AlfrescoDocument checkedOutDocument = (AlfrescoDocument) session.getObject(CmisDocId);
			ContentStream content = new ContentStreamImpl(cmsfilename, null, DetectMimeType(sourcefile, transmeta), file);
		    properties.put(PropertyIds.OBJECT_TYPE_ID, documenttype);
		    if (this.VersioningState.equals(org.apache.chemistry.opencmis.commons.enums.VersioningState.MAJOR)) {
				checkedInDocId = checkedOutDocument.checkIn(true, properties, content, comment);
		    } else {
				checkedInDocId = checkedOutDocument.checkIn(false, properties, content, comment);
		    }
		    setCmsdocument((AlfrescoDocument) session.getObject(checkedInDocId)); 
		} catch (FileNotFoundException e) {
			setMsgError(e.getMessage());
			return false;
		}  finally {
			try {
				file.close();
			} catch (IOException e) {
				/* not interested in error*/
			}
		}
	    if (getCmsdocument()==null){
			return false;
	    } else {
			return true;
	    }
	}
	
	private String DetectMimeType(String sourcefile, TransMeta transmeta){
		//DRH Need to create override option to allow user to specify to skip
		boolean hasMimetype = true;
		final String mimetype="application/pdf";
		if(hasMimetype){
			return mimetype;
		}

		/* TODO change mime type detection to tike - version 1.0 has incompatible classes - so cmis login does not work */
//		FileInputStream is = null;
//		Metadata metadata = null;
//		    try {
//		      File f = new File(sourcefile);
//		      is = new FileInputStream(f);
//
//		      ContentHandler contenthandler = new BodyContentHandler();
//		      metadata = new Metadata();
//		      metadata.set(Metadata.RESOURCE_NAME_KEY, f.getName());
//		      Parser parser = new AutoDetectParser();
//		      // OOXMLParser parser = new OOXMLParser();
//		      parser.parse(is, contenthandler, metadata,null);
////		      System.out.println("Mime: " + metadata.get(Metadata.CONTENT_TYPE));
//		    }
//		    catch (Exception e) {
//		      e.printStackTrace();
//		    }
//		    finally {
//		        if (is != null)
//					try {
//						is.close();
//					} catch (IOException e) {
//						/* not interested in error*/
//					}
//		    }
//		    
//		    if (is != null) {
//		    	return "plain/text";
//		    } else {
//		    	return metadata.get(Metadata.CONTENT_TYPE);
//		    }
		
		String MediaType;
		
		FileObject fileObject = null;
		try {
			fileObject = KettleVFS.getFileObject(sourcefile, transmeta);
		} catch (KettleFileException e) {
//			/* should never occur - as previous checks will reveal all error with this.*/
			e.printStackTrace();
		}
		File f = new File(KettleVFS.getFilename(fileObject));
		
		MimeUtil.registerMimeDetector("eu.medsea.mimeutil.detector.MagicMimeMimeDetector");
		MimeType m = MimeUtil.getMostSpecificMimeType(MimeUtil.getMimeTypes(f));

	    if (m == null) {
			MimeUtil.unregisterMimeDetector("eu.medsea.mimeutil.detector.OpendesktopMimeDetector");
	    	return "plain/text";
	    } else {
			MediaType = m.getMediaType()+"/"+m.getSubType();
			MimeUtil.unregisterMimeDetector("eu.medsea.mimeutil.detector.OpendesktopMimeDetector");
	    	return MediaType;
	    }

	}

//	public Map<String, Integer> GetPropertiesValueList(){
//
//		final Map<String, Integer> fields = new HashMap<String, Integer>();
//		
//		ObjectType type = session.getTypeDefinition("D:erp:document");
////		ObjectType type = session.getTypeDefinition("P:erp:invoiceAspect");
//		Collection<PropertyDefinition<?>> properties = type.getPropertyDefinitions().values();
//		for (PropertyDefinition<?> propertyDefinition : properties) {
//			if (propertyDefinition.getLocalNamespace().equals(type.getLocalNamespace())){
////				System.out.println(propertyDefinition.getId());
////				System.out.println(propertyDefinition.getPropertyType());
//				if (propertyDefinition.getExtensions() != null){
//					List<CmisExtensionElement> choices = propertyDefinition.getExtensions();
//					for (int i = 0; i < choices.size(); i++) {
//						List<CmisExtensionElement> list = propertyDefinition.getExtensions().get(i).getChildren();
//						for (CmisExtensionElement cmisExtensionElement : list) {
////							System.out.println("Choice "+ i + " :" + cmisExtensionElement.getValue());
//							fields.put(cmisExtensionElement.getValue(), i);
//						}
//					}
//				}
//			}		
//		}
//		return fields;
//	}
		

	public void setCmisDialogProperties(TableView wMetaDataList,String documenttype) {
		int i=0;
		wMetaDataList.removeAll();
		wMetaDataList.table.removeAll();
		
		while ((documenttype!=null) && (!documenttype.isEmpty())){
			ItemIterable<ObjectType> types = session.getTypeChildren(documenttype, true); 
			for( ObjectType type2 : types) { 
				Map<String, PropertyDefinition<?>> properties = type2.getPropertyDefinitions();
				for (Map.Entry<String,PropertyDefinition<?>> entry : properties.entrySet()) {
					PropertyDefinition<?> propertyDefinition = entry.getValue();
					if(!(propertyDefinition.getId().equals(PropertyIds.NAME)) && !(propertyDefinition.getId().equals(PropertyIds.OBJECT_TYPE_ID))){
//						propertyDefinition.getUpdatability();
						if(propertyDefinition.getUpdatability()!=Updatability.READONLY){
							/* skip name, because already defined elsewhere & object id */
							Table items = wMetaDataList.getTable();
							Integer count = items.getItemCount();
							TableItem item = new TableItem(items, SWT.NONE);
							item.setText(0, Integer.toString(i));
							item.setText(2, Const.NVL(propertyDefinition.getId(), ""));
							item.setText(3, Const.NVL(propertyDefinition.getUpdatability().name(), ""));
							item.setText(4, Const.NVL(propertyDefinition.getCardinality().name(), ""));
							item.setText(5, Const.NVL(propertyDefinition.getLocalName(), ""));
							item.setText(6, Const.NVL(documenttype, ""));
							item.setText(7, Const.NVL(propertyDefinition.getPropertyType().name(), ""));
							i+=1;
						}
					}	
				} 					     
			}			
			/* get the parent document type */
			documenttype = session.getTypeDefinition(documenttype).getParentType().getId();
			if ((documenttype.equals("cmis:document")) || (documenttype.equals("cmis:policy"))){
				documenttype = null;
				}
		}
		
	}	
	
	public void setCmisDialogAspectProperties(TableView wMetaDataList,String documenttype) {
		int i=wMetaDataList.table.getItemCount();
		
		ObjectType type2 = session.getTypeDefinition(documenttype);
		Map<String, PropertyDefinition<?>> properties = type2.getPropertyDefinitions();
		for (Map.Entry<String,PropertyDefinition<?>> entry : properties.entrySet()) {
			PropertyDefinition<?> propertyDefinition = entry.getValue();
			if(!(propertyDefinition.getId().equals(PropertyIds.NAME)) && !(propertyDefinition.getId().equals(PropertyIds.OBJECT_TYPE_ID))){
				propertyDefinition.getUpdatability();
				if(propertyDefinition.getUpdatability()!=Updatability.READONLY){
					/* skip name, because already defined elsewhere & object id */
					Table items = wMetaDataList.getTable();
					Integer count = items.getItemCount();
					TableItem item = new TableItem(items, SWT.NONE);
					item.setText(0, Integer.toString(i));
					item.setText(2, Const.NVL(propertyDefinition.getId(), ""));
					item.setText(3, Const.NVL(propertyDefinition.getUpdatability().name(), ""));
					item.setText(4, Const.NVL(propertyDefinition.getCardinality().name(), ""));
					item.setText(5, Const.NVL(propertyDefinition.getLocalName(), ""));
					item.setText(6, Const.NVL(documenttype, ""));
					item.setText(7, Const.NVL(propertyDefinition.getPropertyType().name(), ""));
					i+=1;
//					wMetaDataList.table.setItemCount(i);
				}
			}	
		}
	}	
}
