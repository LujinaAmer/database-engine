import java.io.BufferedReader;
//import org.junit.jupiter.api.Assertions;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.security.Key;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;

import javax.net.ssl.SSLEngineResult.HandshakeStatus;

import Exceptions.DBAppException;
import Exceptions.DuplicateException;
import Exceptions.NotFoundException;

public class DBApp {



	public void init() {
		try (OutputStream output = new FileOutputStream("src/resources/DBApp.config")) {

			Properties prop = new Properties();

			// set the properties value
			prop.setProperty("MaximumRowsCountinTablePage", "200");
			prop.setProperty("MaximumEntriesinOctreeNode", "16");
			// save properties to project root folder
			prop.store(output, null);

		} catch (IOException io) {
			io.printStackTrace();
		}

	}
	// this does whatever initialization you would like
	// or leave it empty if there is no code you want to
	// execute at application startup

	// following method creates one table only
	// strClusteringKeyColumn is the name of the column that will be the primary
	// key and the clustering column as well. The data type of that column will
	// be passed in htblColNameType
	// htblColNameValue will have the column name as key and the data
	// type as value
	// htblColNameMin and htblColNameMax for passing minimum and maximum values
	// for data in the column. Key is the name of the column
	public void createTable(String strTableName, String strClusteringKeyColumn,
			Hashtable<String, String> htblColNameType,
			Hashtable<String, String> htblColNameMin,
			Hashtable<String, String> htblColNameMax)
					throws DBAppException {

		boolean flag = false;
		try (BufferedReader br = new BufferedReader(new FileReader("metadata.csv"));) {
			// read metadata
			// BufferedReader br = new BufferedReader(new FileReader("metadata.csv"));
			String line = br.readLine();
			while (line != null) {
				String[] content = line.split(",");
				if (strTableName.equalsIgnoreCase(content[0])) {
					// br.close();
					flag = true;
					throw new DuplicateException("table name already exists");
				}
				line = br.readLine();
			}
			// br.close();
		} catch (IOException e) {

			System.out.println("creating metadata...");

		}

		if (flag) {
			return; // table already created
		}

		// write metadata
		try {
			FileWriter meta = new FileWriter("metadata.csv", true);
			PrintWriter writer = new PrintWriter(meta);

			// insert in metadata
			Enumeration<String> keys = htblColNameType.keys();
			while (keys.hasMoreElements()) {

				String key = keys.nextElement();

				if (key.equalsIgnoreCase(strClusteringKeyColumn)) {
					writer.println(strTableName + "," + key + "," + htblColNameType.get(key) + "," + "True," + "null,"
							+ "null," + htblColNameMin.get(key)
							+ "," + htblColNameMax.get(key));
				}

				else {
					writer.println(strTableName + "," + key + "," + htblColNameType.get(key) + "," + "False," + "null,"
							+ "null," + htblColNameMin.get(key)
							+ "," + htblColNameMax.get(key));
				}
			}
			writer.close();

			Table table = new Table(strTableName);
			saveTable(table);
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}

	}



	public void createIndex(String strTableName, String[] strarrColName) throws DBAppException {

		if (strarrColName.length != 3 ) { 
			// && strarrColName.length % 3 == 0) {
			throw new DBAppException("Need 3 columns to create an index");
		}

		for (int i =0 ; i < strarrColName.length ; i++ ){ //check for duplicates 
			for(int j = i+1 ; j < strarrColName.length ; j++) {
				if (strarrColName[i].equals(strarrColName[j])) {
					throw new DBAppException("Need only 3 columns to create an index") ;
				}
			}
		}

		boolean tableName = false ; // find table in metadata

		try {

			String[] min = new String[6] ;
			String[] max = new String[6] ;

			BufferedReader br0 = new BufferedReader(new FileReader("metadata.csv")); // handle lower/upper case
			String line0 = br0.readLine();
			while (line0 != null) {
				String[] content = line0.split(",");
				if (strTableName.equalsIgnoreCase(content[0]) ) {
					if (strarrColName[0].equalsIgnoreCase(content[1]) ) {
						strarrColName[0]= content[1] ;
					} 
					if(strarrColName[1].equalsIgnoreCase(content[1])) {
						strarrColName[1]= content[1] ;
					}
					if ( strarrColName[2].equalsIgnoreCase(content[1]) ) {
						strarrColName[2]= content[1] ;
					}

				}
				line0 = br0.readLine();
			}
			br0.close();
			BufferedReader br = new BufferedReader(new FileReader("metadata.csv")); // validate table , col names

			FileWriter temp = new FileWriter("temp.csv", false);
			PrintWriter writer = new PrintWriter(temp);

			int colFlag = 0 ; //  find the col 
			String line = br.readLine();
			while (line != null) {  
				String[] content = line.split(",");
				if (strTableName.equalsIgnoreCase(content[0]) ) {
					tableName = true ;
					if ((strarrColName[0].equalsIgnoreCase(content[1]) ||strarrColName[1].equalsIgnoreCase(content[1]) 
							|| strarrColName[2].equalsIgnoreCase(content[1])) && content[5].equals("null") ) {
						writer.println(content[0] + "," + content[1] + "," + content[2] + "," + content[3] + "," +
								strarrColName[0]+"-"+strarrColName[1]+"-"+
								strarrColName[2]+"-Index" +","+ "Octree," 
								+ content[6]+ "," + content[7] );
						colFlag++;
						if(strarrColName[0].equalsIgnoreCase(content[1])) {
							strarrColName[0] = content[1];
							min[0] =  content[6];
							max[0] =  content[7];
							min[1] = content[2];
						}
						if(strarrColName[1].equalsIgnoreCase(content[1])) {
							strarrColName[1] = content[1];
							min[2] =  content[6];
							max[2] =  content[7];
							min[3] = content[2];
						}
						if(strarrColName[2].equalsIgnoreCase(content[1])) {
							strarrColName[2] = content[1];
							min[4] =  content[6];
							max[4] =  content[7];
							min[5] = content[2];
						}
					}
					else {
						writer.println(line);
					}
				}
				else {
					writer.println(line);
				}

				line = br.readLine();
			} // end of loop 

			br.close();
			writer.close();

			if(colFlag ==3) {
				File meta = new File("metadata.csv");
				meta.delete();

				File newMeta = new File("temp.csv");
				newMeta.renameTo(new File("metadata.csv"));

			}
			else {
				File tempFile = new File("temp.csv");
				tempFile.delete();
				throw new DBAppException("Error creating index");
			}



			if (!tableName) {
				throw new NotFoundException("Table not found");
			}


			Table table = loadTable(strTableName) ; 
			Octree index = new Octree(fromstoobj(min[0],min[1]), fromstoobj(min[2],min[3]), fromstoobj(min[4],min[5])
					, fromstoobj(max[0],min[1]),fromstoobj(max[2],min[3]), fromstoobj(max[4],min[5]));

			table.createIndex(index , strarrColName[0], strarrColName[1] , strarrColName[2]);

			// if table already have records
			saveIndex(index, strarrColName[0]+"-"+strarrColName[1]+"-"+ strarrColName[2]+"-Index"  );

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


	}

	private Object fromstoobj(String strClusteringKeyValue , String type) {
		Object result = null;
		if(type.equals("java.lang.Integer")) {
			result = (java.lang.Integer)Integer.parseInt(strClusteringKeyValue);
		}

		if(type.equals("java.lang.Double")) {
			result =(java.lang.Double) Double.parseDouble(strClusteringKeyValue);
		}

		if(type.equals("java.util.Date")) {
			
				result =(java.util.Date)new Date(strClusteringKeyValue);
			
		}
		if(type.equals("java.lang.String")) {
			result = (java.lang.String) strClusteringKeyValue;
		}
		return result;
	}

	public void insertIntoTable(String strTableName,
			Hashtable<String, Object> htblColNameValue)
					throws DBAppException {

		try {
			if (isValid(strTableName, htblColNameValue)) {
				ArrayList<String> arr = exists(htblColNameValue, strTableName);
				if (arr != null) {

					for (int i = 0; i < arr.size(); i++) {
						Enumeration<String> keys = htblColNameValue.keys();
						while (keys.hasMoreElements()) {
							String key = keys.nextElement();
							if (key.equalsIgnoreCase(arr.get(i))) {
								Object o = htblColNameValue.get(key);
								htblColNameValue.remove(key);
								htblColNameValue.put(arr.get(i), o);
							}
						}
							htblColNameValue.putIfAbsent(arr.get(i), new Null());
					}
					
					if (arr.size() == htblColNameValue.size()) {
						String pkIndex = "" ;

						BufferedReader br1 = new BufferedReader(new FileReader("metadata.csv"));
						String line1 = br1.readLine();

						Vector<String[]>  s = new Vector<>();
						String [] resAll = new String [5];		//x,y,z , index name, pk , type 
						while (line1 != null) {
							String[] content = line1.split(",");
							if(content[4].equals("null")) {
								line1 = br1.readLine();
								continue;
							}

							String [] index = content[4].split("-");

							if( content[3].equals("True")) {
								pkIndex = content[1];
							}

							if (strTableName.equals(content[0]) && !content[4].equals("null")  ) {
								resAll[0] = index[0];
								resAll[1] = index[1];
								resAll[2] = index[2];
								resAll[3] = content[4];
								resAll[4] = content[2];
								if(!s.contains(resAll)) {
									s.add(resAll);
								}
							}

							line1 = br1.readLine();
						}
						br1.close();
						Table table = loadTable(strTableName);
						ArrayList<Object> returned = table.insert(htblColNameValue);
						saveTable(table);

						for(int i =0 ; i<s.size() ; i++) {
							Octree tree = loadIndex(s.get(i)[3]);

							Vector<Object[]> v = new Vector<>();
							Object[] o = {htblColNameValue.get(pkIndex), returned.get(0)};
							v.add(o);

							Object[] in = {htblColNameValue.get(s.get(i)[0]),
									htblColNameValue.get(s.get(i)[1]), 
									htblColNameValue.get(s.get(i)[2]),v};

							tree.insert(htblColNameValue.get(s.get(i)[0]),
									htblColNameValue.get(s.get(i)[1]), 
									htblColNameValue.get(s.get(i)[2]),
									in
									);
							saveIndex(tree , s.get(i)[3]);

						}
						for(int i = 1 ; i < returned.size();i++) {
							for(int i1 =0 ; i1<s.size() ; i1++) {
								Octree tree = loadIndex(s.get(i1)[3]);

								Vector<Object[]> v = new Vector<>();
								Object[] zrbw = (Object[])returned.get(i);
								Object[] o = {((Hashtable<String, Object>)zrbw[0]).get(pkIndex), zrbw[1]};
								v.add(o);

								Object[] in = {htblColNameValue.get(s.get(i1)[0]),
										htblColNameValue.get(s.get(i1)[0]), 
										htblColNameValue.get(s.get(i1)[0]),v};

								tree.update(((Hashtable<String, Object>)zrbw[0]).get(s.get(i1)[0]), 
										((Hashtable<String, Object>)zrbw[0]).get(s.get(i1)[1]), 
										((Hashtable<String, Object>)zrbw[0]).get(s.get(i1)[2]), 
										((Hashtable<String, Object>)zrbw[0]).get(pkIndex));

								tree.insert(((Hashtable<String, Object>)zrbw[0]).get(s.get(i1)[0]), 
										((Hashtable<String, Object>)zrbw[0]).get(s.get(i1)[1]), 
										((Hashtable<String, Object>)zrbw[0]).get(s.get(i1)[2]),
										in
										);
								saveIndex(tree , s.get(i1)[3]);
							}
						}
					}
						else {
						throw new NotFoundException("Column not found");
					}

				} else {
					throw new DBAppException("Primary Key Cannot Be Empty");
				}
			} else {
				throw new DBAppException("Wrong entry");
			}
		} catch (IOException e) {
			System.out.println("Table Not Found");
		} catch (ParseException e) {
			System.out.println(e.getMessage());
		}

	}

	public Boolean isValid(String strTableName, Hashtable<String, Object> htblColNameValue)
			throws IOException, ParseException, NotFoundException {

		boolean flag = false; // DoWEHaveThistable

		BufferedReader br = new BufferedReader(new FileReader("metadata.csv"));
		String line = br.readLine();
		while (line != null) {
			String[] content = line.split(",");
			if (strTableName.equalsIgnoreCase(content[0])) {
				flag = true; // found the table
				Enumeration<String> keys = htblColNameValue.keys();
				while (keys.hasMoreElements()) {
					String key = keys.nextElement();
					if (key.equalsIgnoreCase(content[1])) {
						if (!htblColNameValue.get(key).getClass().getName().equalsIgnoreCase(content[2])) {
							System.out.println(content[2]);
							System.out.println("1");
							br.close();
							return false;
						}
						if (comparing(htblColNameValue.get(key), content[6]) < 0
								|| comparing(htblColNameValue.get(key), content[7]) > 0) {
							System.out.println("2");
							br.close();
							return false;
						}
					}
				}
			}
			line = br.readLine();
		}
		br.close();
		if (flag) {
			return true;
		} else {
			throw new NotFoundException("Table Not Found");
		}

	}

	private Double comparing(Object value, String compared) throws ParseException {
		Double difference = 0.0;

		if (value.getClass().getName().equals("java.lang.Integer")) {
			difference += ((java.lang.Integer) value - Integer.parseInt(compared));
			return difference;
		}

		if (value.getClass().getName().equals("java.lang.Double")) {
			difference = ((java.lang.Double) value - Double.parseDouble(compared));
			return difference;
		}

		if (value.getClass().getName().equals("java.util.Date")) {
			Date date1 = new SimpleDateFormat("dd/MM/yyyy").parse(compared);
//			Date date1 = new Date (compared); 
			difference += ((java.util.Date) value).compareTo(date1);
			return difference;
		}

		if (value.getClass().getName().equals("java.lang.String")) {
			difference += ((java.lang.String) value).compareTo(compared);
			return difference;
		}
		return difference;
	}

	// following method updates one row only
	// htblColNameValue holds the key and new value
	// htblColNameValue will not include clustering key as column name
	// strClusteringKeyValue is the value to look for to find the row to update.
	public void updateTable(String strTableName, String strClusteringKeyValue,
			Hashtable<String, Object> htblColNameValue) throws DBAppException {


		//ArrayList<String> arr = new ArrayList<String>();
		//int i = 0;
//		try { // validate
//			BufferedReader br = new BufferedReader(new FileReader("metadata.csv"));
//			String line = br.readLine();
//			while (line != null) {
//				String[] content = line.split(",");
//				if(content[0].equalsIgnoreCase(strTableName)) {
//					String o = content[1];
//					arr.add(i, o);
//				}
//				line = br.readLine();
//				i++;
//			}
//			br.close();

//			for (int j = 0; j < arr.size(); j++) {
//				Enumeration<String> keys1 = htblColNameValue.keys();
//				while (keys1.hasMoreElements()) {
//					String key = keys1.nextElement();
//					if (key.equalsIgnoreCase(arr.get(j))) {
//						Object o = htblColNameValue.get(key);
//						htblColNameValue.remove(key);
//						htblColNameValue.put(arr.get(j), o);
//					}
//				}
//			}

//			Hashtable<String, Object> test = new Hashtable<String, Object>();
//
//			Enumeration<String> keys = htblColNameValue.keys();
//			while (keys.hasMoreElements()) {
//				String key = keys.nextElement();
//				test.put(key, htblColNameValue.get(key));
//			}
//
//			for (int j = 0; j < arr.size(); j++) {
//
//				test.putIfAbsent(arr.get(j), new Null());
//			}
			try {
			//if (arr.size() == test.size()) {

				if (isValid(strTableName, htblColNameValue)) { // end of validate
					try {

						/////////////////////////////////////
						// index 
						boolean useIndex = false ;
						String pkIndex = "" ;

						BufferedReader br1 = new BufferedReader(new FileReader("metadata.csv"));
						String line1 = br1.readLine();

						Vector<String[]>  s = new Vector<>();
						String [] resAll = new String [5];
						String [] res = new String[5] ;  //x,y,z , index name, pk , type 
						while (line1 != null) {
							String[] content = line1.split(",");
							if(content[4].equals("null")) {
								line1 = br1.readLine();
								continue;
							}

							String [] index = content[4].split("-");
							if (strTableName.equals(content[0]) && !content[4].equals("null")  ) {
								if( content[3].equals("True")) {
									useIndex = true;
									pkIndex = content[1];
									res[0] = index[0];
									res[1] = index[1];
									res[2] = index[2];
									res[3] = content[4];
									res[4] = content[2];
									s.add(res);
								}else {
									resAll[0] = index[0];
									resAll[1] = index[1];
									resAll[2] = index[2];
									resAll[3] = content[4];
									resAll[4] = content[2];
									if(!s.contains(resAll)) {
										s.add(resAll);
									}
								}

							}

							line1 = br1.readLine();
						}
						br1.close();
						Table table = loadTable(strTableName);
						if(false) {
						Object newPk =null;
						if(res[4].equals("java.lang.Integer")) {
							newPk = (java.lang.Integer)Integer.parseInt(strClusteringKeyValue);
						}

						if(res[4].equals("java.lang.Double")) {
							newPk =(java.lang.Double) Double.parseDouble(strClusteringKeyValue);
						}

						if(res[4].equals("java.util.Date")) {
							
								newPk =(java.util.Date) new Date(strClusteringKeyValue);
							
						}
						if(res[4].equals("java.lang.String")) {
							newPk = (java.lang.String) strClusteringKeyValue;
						}




						

						

							Octree tree =  loadIndex(res[3]); // update octree 
							Object[] result = null ;

							if(pkIndex.equals(res[0])) {
								result = tree.update(newPk , null , null , newPk ) ;
							}
							else if(pkIndex.equals(res[1])) {
								result =tree.update(null , newPk , null , newPk ) ;
							}
							else if(pkIndex.equals(res[2])) {
								result =tree.update(null , null , newPk , newPk );
								//htblColNameValue.get(res[0]) ,htblColNameValue.get(res[1]) , htblColNameValue.get(res[2])) ;
							}

							Hashtable<String, Object> newRow = table.updateIndex(result , htblColNameValue);
							//tree.insert(pkIndex, strClusteringKeyValue, newValues, res)
							Vector<Object[]> v = new Vector<>();
							v.add(result);
							Object[] record = {newRow.get(res[0]) , newRow.get(res[1]) , newRow.get(res[2]) , v};
							tree.insert(newRow.get(res[0]) , newRow.get(res[1]) , newRow.get(res[2]), record);
							saveIndex(tree,res[3]);

						}
						else {
							//without index
							///////////////////////////////////////////////

							Object[] returned = table.update(strTableName, strClusteringKeyValue, htblColNameValue);
							//{ pk , id , page.get(position) }
							Hashtable<String, Object> newRow = (Hashtable<String, Object>) returned[2];
							saveTable(table);
							
							for(int i = 0 ; i < s.size() ; i ++) {
								if( htblColNameValue.containsKey(s.get(i)[0]) 
										||htblColNameValue.containsKey(s.get(i)[1])
										||htblColNameValue.containsKey(s.get(i)[2])) {

									Octree tree =  loadIndex(s.get(i)[3]);

									Vector<Object[]> v = new Vector<>();
									Object[] result = {returned[0] ,returned[1]};
									v.add(result);
									Object[] record = { newRow.get(s.get(i)[0]) 
											, newRow.get(s.get(i)[1]) 
											, newRow.get(s.get(i)[2]) , v};

									tree.insert(newRow.get(s.get(i)[0]) 
											, newRow.get(s.get(i)[1]) 
											, newRow.get(s.get(i)[2]), record);
									saveIndex(tree , s.get(i)[3]);
								}
							}
							//Table table = loadTable(strTableName);

						}
					} catch (NotFoundException e) {
						e.getMessage();
					}
				} else {
					throw new DBAppException("wrong entry");
				}
//			} else {
//				throw new NotFoundException("Column not found");
//			}
		} catch (IOException e) {
			System.out.println("Table Not Found");
		} catch (ParseException e) {
			System.out.println(e.getMessage());
		}

	}

	// following method could be used to delete one or more rows.
	// htblColNameValue holds the key and value. This will be used in search
	// to identify which rows/tuples to delete.
	// htblColNameValue enteries are ANDED together
	public void deleteFromTable(String strTableName,Hashtable<String, Object> htblColNameValue)
			throws DBAppException {

//		ArrayList<String> arr = new ArrayList<String>();
//		int i = 0;
		try {
//			BufferedReader br = new BufferedReader(new FileReader("metadata.csv"));
//			String line = br.readLine();
//			while (line != null) {
//				String[] content = line.split(",");
//				if(content[0].equalsIgnoreCase(strTableName)) {
//					String o = content[1];
//					arr.add(i, o);
//				}
//				line = br.readLine();
//				i++;
//			}
//			br.close();
//
//			for (int j = 0; j < arr.size(); j++) {
//				Enumeration<String> keys1 = htblColNameValue.keys();
//				while (keys1.hasMoreElements()) {
//					String key = keys1.nextElement();
//					if (key.equalsIgnoreCase(arr.get(j))) {
//						Object o = htblColNameValue.get(key);
//						htblColNameValue.remove(key);
//						htblColNameValue.put(arr.get(j), o);
//					}
//				}
//			}
//
//			Hashtable<String, Object> test = new Hashtable<String, Object>();
//
//			Enumeration<String> keys = htblColNameValue.keys();
//			while (keys.hasMoreElements()) {
//				String key = keys.nextElement();
//				test.put(key, htblColNameValue.get(key));
//			}
//
//			for (int j = 0; j < arr.size(); j++) {
//
//				test.putIfAbsent(arr.get(j), new Null());
//			}
//
//			if (arr.size() == test.size()) {

				if (isValid(strTableName, htblColNameValue)) {

					String pkIndex = "";
					BufferedReader br1 = new BufferedReader(new FileReader("metadata.csv"));
					String line1 = br1.readLine();
					Vector<String[]>  s = new Vector<>();
					String [] resAll = new String [5];		//x,y,z , index name, pk , type 
					while (line1 != null) {
						String[] content = line1.split(",");
						if(content[4].equals("null")) {
							line1 = br1.readLine();
							continue;
						}

						String [] index = content[4].split("-");

						if( content[3].equals("True")) {
							pkIndex = content[1];
						}

						if (strTableName.equals(content[0]) && !content[4].equals("null")  ) {
							resAll[0] = index[0];
							resAll[1] = index[1];
							resAll[2] = index[2];
							resAll[3] = content[4];
							resAll[4] = content[2];
							if(!s.contains(resAll)) {
								s.add(resAll);
							}
						}

						line1 = br1.readLine();
					}
					br1.close();

					Vector<String> z = new Vector();
					Enumeration<String> keys1 = htblColNameValue.keys();
					while (keys1.hasMoreElements()) {
						String key1 = keys1.nextElement();
						z.add(key1);
					}
					Object[] columnorder = null;
					for(int i =0 ; i<z.size()-2;i++) {
						columnorder = haveIndex(strTableName , z.get(i), z.get(i+1), z.get(i+2));
						if(columnorder!=null) {
							break;
						}
					}

					if(columnorder != null) {
						Octree tree = loadIndex(columnorder[0]+
								"-"+columnorder[1]
								+"-"+columnorder[2]+"-Index");

						Object[] returned = tree.get(htblColNameValue.get(columnorder[0]),
								htblColNameValue.get(columnorder[1]),
								htblColNameValue.get(columnorder[2]));
						
						Table table = loadTable(strTableName);
						Hashtable<String, Object>  v = new Hashtable<String, Object>();
						boolean flag = false;
						for (Object[] object : (Vector<Object[]>) returned[3]) {
							for(int j = 0 ; j < z.size() ; j++ ) {
								flag = false;
								if(comparing(table.getRow(object).get(z.get(j)),htblColNameValue.get(z.get(j))) ==0 ){
									flag = true;
								}
								else{
									flag = false;
									break;
								}
							}
							if(flag = true ) {
								table.deleteindex(object);
							}
						}
					}
					else {
						Table table = loadTable(strTableName);
						int deleted = table.delete(htblColNameValue);
						if (deleted == -1) {
							System.out.println("All Pages Deleted");
						} else if (deleted == 0) {
							throw new NotFoundException("tuple not found");
						} else {
							System.out.println("Number of deleted items: " + deleted);
						}
						saveTable(table);
					}





					for(int i1 =0 ; i1<s.size() ; i1++) {
						Octree tree = loadIndex(s.get(i1)[3]);

						tree.remove(htblColNameValue.get(s.get(i1)[0]),
								htblColNameValue.get(s.get(i1)[1]), 
								htblColNameValue.get(s.get(i1)[2]));

						saveIndex(tree , s.get(i1)[3]);
					}


				} else {
					throw new DBAppException("Wrong entry");
				}
//			} else {
//				throw new NotFoundException("Column Not Found");
//			}
		} catch (IOException e) {
			System.out.println("Table Not Found");
		} catch (ParseException e) {
			System.out.println(e.getMessage());
		}

	}


	public Iterator selectFromTable(SQLTerm[] arrSQLTerms, String[] strarrOperators) throws DBAppException {



		for (int i =0 ; i < arrSQLTerms.length ; i++) {// validate the entries

			boolean tableName = false ;
			boolean columnExist = false ;
			boolean objType = false ;
			try {

				BufferedReader br = new BufferedReader(new FileReader("metadata.csv"));
				String line = br.readLine();

				while (line != null) {
					String[] content = line.split(",");
					if (arrSQLTerms[i]._strTableName.equals(content[0])) {
						tableName = true ;
						if (arrSQLTerms[i]._strColumnName.equals(content[1])) {

							columnExist = true ;

							if (arrSQLTerms[i]._objValue.getClass().getName().equals(content[2])) {
								objType = true ;
							}
						}

					}
					line = br.readLine();
				}
				br.close();

				if (!tableName) {
					throw new DBAppException("Table not found");
				}
				if (!columnExist) {
					throw new DBAppException("Column not found");
				}
				if (!objType) {
					throw new DBAppException("Wrong data type");
				}

				if (!arrSQLTerms[i]._strOperator.equals(">") && !arrSQLTerms[i]._strOperator.equals(">=") &&
						!arrSQLTerms[i]._strOperator.equals("<") && !arrSQLTerms[i]._strOperator.equals("<=") &&
						!arrSQLTerms[i]._strOperator.equals("!=") && !arrSQLTerms[i]._strOperator.equals("=")) {
					throw new DBAppException(arrSQLTerms[i]._strOperator +" is not valid operator");
				}



			}catch (IOException e) {
				// TODO: handle exception
				System.out.println(e.getMessage());
			}
		} 



		for (int i =0 ; i < strarrOperators.length ; i ++) {
			if (!strarrOperators[i].equals("AND") && !strarrOperators[i].equals("OR") &&
					!strarrOperators[i].equals("XOR")) {
				throw new DBAppException(strarrOperators[i] +" is not supported");
			}
		}// end of validate the entries


		////////////////////////////////////
		ArrayList<String> arr = new ArrayList<String>(); // lower/upper case
		int i = 0;
		Hashtable<String, String> columnstype = new Hashtable<>();
		try { 
			BufferedReader br = new BufferedReader(new FileReader("metadata.csv"));
			String line = br.readLine();
			while (line != null) {
				String[] content = line.split(",");

				if(content[0].equalsIgnoreCase(arrSQLTerms[0]._strTableName)) {
					columnstype.put(content[1], content[2]);
					String o = content[1];
					arr.add(i, o);
				}
				line = br.readLine();
				i++;
			}
			br.close();

			for (int j = 0; j < arr.size(); j++) {

				for(int k =0 ; k < arrSQLTerms.length ; k++) {
					if (arrSQLTerms[k]._strColumnName.equalsIgnoreCase(arr.get(j))) {
						arrSQLTerms[k]._strColumnName = arr.get(j) ;
					}
				}
			}

		}catch (IOException  e) {
			// TODO: handle exception
		}
		Vector<Hashtable<String, Object>> v= new Vector<Hashtable<String, Object>>() ;

		SQLTerm[] ColumnOrder = null;
		if(arrSQLTerms.length>=3){
			if(strarrOperators[0].equals("AND") && strarrOperators[1].equals("AND") ) {
				ColumnOrder = haveIndexSelect( arrSQLTerms[0] ,
						arrSQLTerms[1] , arrSQLTerms[2]);
			}

		}

		try {
			Table table = loadTable(arrSQLTerms[0]._strTableName);

			if(ColumnOrder!= null){
				Octree tree = loadIndex(ColumnOrder[0]._strColumnName+
						"-"+ColumnOrder[1]._strColumnName
						+"-"+ColumnOrder[2]._strColumnName+"-Index");

				Object[] inx = {ColumnOrder[0]._objValue,ColumnOrder[0]._strOperator};
				Object[] iny = {ColumnOrder[1]._objValue,ColumnOrder[1]._strOperator};
				Object[] inz = {ColumnOrder[2]._objValue,ColumnOrder[2]._strOperator};
				Vector<Object[]> O = tree.select(inx, iny, inz);
				for (Object[] objects : O) {// {x,y,z, <[pk,id][pk,id][pk,id]>}
					for (Object[] objects2 : (Vector<Object[]>)objects[3]) { 
						v.add(table.getRow(objects2));
					}
				}
				//v.add();
			}
			else{
				v = table.select( arrSQLTerms[0] ) ;
			}
			int blah = 0;
			if(ColumnOrder == null) {
				blah = 1;
			}	
			else {
				blah = 3;
			}
			for (int i1 = blah ; i1 < arrSQLTerms.length ; i1++) {
				ColumnOrder = null;
				if(arrSQLTerms.length-i1 >=3){
					ColumnOrder = haveIndexSelect(  arrSQLTerms[i1], 
							arrSQLTerms[i1+1], arrSQLTerms[i1+2]);		

					if(ColumnOrder!=null){
						Octree tree = loadIndex(ColumnOrder[0]._strColumnName+
								"-"+ColumnOrder[1]._strColumnName
								+"-"+ColumnOrder[2]._strColumnName+"-Index");
						Object[] inx = {ColumnOrder[0]._objValue,ColumnOrder[0]._strOperator};
						Object[] iny = {ColumnOrder[1]._objValue,ColumnOrder[1]._strOperator};
						Object[] inz = {ColumnOrder[2]._objValue,ColumnOrder[2]._strOperator};
						Vector<Object[]> O = tree.select(inx, iny, inz);
						Vector<Hashtable<String, Object>> temp= new Vector<Hashtable<String, Object>>();
						for (Object[] objects : O) {
							for (Object[] objects2 : (Vector<Object[]>)objects[3]) { 
								temp.add(table.getRow(objects2));
							}
						}
						comparevectors(temp, strarrOperators[i-1], v);
						i1 += 2;
					}	
					else{
						table.select( arrSQLTerms[i1]  , strarrOperators[i1-1] ,v );
					}

				}
				else{

					if(ColumnOrder==null){
						table.select( arrSQLTerms[i1]  , strarrOperators[i1-1] ,v );
					}
				}

			}



		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Iterator<Hashtable<String, Object>> t = v.iterator() ;


		return t ;



	}

	public void comparevectors(Vector<Hashtable<String, Object>> v, String operator,
			Vector<Hashtable<String, Object>> result) { 

		// in case of "and", we will loop over the vector ,adding what satisfies the SQL term in temp
		if (operator.equals("AND")) { 
			Vector<Hashtable<String, Object>> temp = new Vector<Hashtable<String, Object>>() ;

			for (Hashtable<String, Object> values : v) {
				if(result.contains(values)) {
					temp.add(values);
				}
			}
			result = temp ;

		}
		else if (operator.equals("OR")) {
			// only if it result !contains it
			for (Hashtable<String, Object> values : v) {
				if(!result.contains(values)) {
					result.add(values);
				}
			}


		}
		else if (operator.equals("XOR")) {

			// then we merge temp and result , considering the duplicates

			Vector<Hashtable<String, Object>> temp = new Vector<Hashtable<String, Object>>() ;
			for (Hashtable<String, Object> values : v) {
				if(!result.contains(values)) {
					temp.add(values);
				}
			}
			for (Hashtable<String, Object> values : result) {
				if(!v.contains(values)) {
					temp.add(values);
				}
			}
			result = temp;
		}//end of XOR case

	}// end of selectFromPage()

	private Double comparing(Object value , Object compared) {
		Double difference = 0.0;
		if(value.toString().equals("Null") || compared.toString().equals("Null")) {
			return null;
		}
		if(value.getClass().getName().equals("java.lang.Integer")) {
			difference += (java.lang.Integer)value - (java.lang.Integer)compared;
			return difference;
		}

		if(value.getClass().getName().equals("java.lang.Double")) {
			difference = (java.lang.Double)value - (java.lang.Double)compared;
			return difference;
		}

		if(value.getClass().getName().equals("java.util.Date")) {
			difference += ((java.util.Date)value).compareTo((java.util.Date)compared);
			return difference;
		}

		if(value.getClass().getName().equals("java.lang.String")) {
			difference += ((java.lang.String)value).compareTo((java.lang.String)compared);
			return difference;
		}
		return difference;
	}


	public ArrayList<String> exists(Hashtable<String, Object> tuple, String strTableName) throws IOException {
		boolean pk = false;
		ArrayList<String> arr = new ArrayList<String>();
		int i = 0;
		BufferedReader br = new BufferedReader(new FileReader("metadata.csv"));
		String line = br.readLine();
		while (line != null) {
			String[] content = line.split(",");
			String o = content[1];
			arr.add(i, o);
			if (strTableName.equalsIgnoreCase(content[0])) {
				Enumeration<String> keys = tuple.keys();
				while (keys.hasMoreElements()) {
					String key = keys.nextElement();
					if (key.equalsIgnoreCase(content[1])) {
						arr.remove(i);
						o = content[1];
						arr.add(i, o);
						if (content[3].equalsIgnoreCase("True")) {
							pk = true;
						}
					}
				}
			}

			line = br.readLine();
			i++;
		}
		br.close();

		if (!pk) {
			return null;
		}
		return arr;

	}

	public void saveTable(Table table) throws IOException {
		FileOutputStream fout = new FileOutputStream(table.getName() + ".class");
		ObjectOutputStream oos = new ObjectOutputStream(fout);
		oos.writeObject(table);
		oos.close();
	}



	public Table loadTable(String tablename) throws IOException {
		// FileInputStream fiut = new FileInputStream(tablename + ".class");
		// ObjectInputStream ois = new ObjectInputStream(fiut);
		try (FileInputStream fiut = new FileInputStream(tablename + ".class");
				ObjectInputStream ois = new ObjectInputStream(fiut);) {
			return (Table) ois.readObject();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}

	public SQLTerm[] haveIndexSelect ( SQLTerm x , SQLTerm y , SQLTerm z) {
		//		System.out.println("da5al");
		String[] res = new String [3];
		SQLTerm[] order = null;
		boolean flag = false;
		try {
			BufferedReader br = new BufferedReader(new FileReader("metadata.csv"));
			String line = br.readLine();

			int i =0 ;
			while (line != null) {
				String[] content = line.split(",");
				if(content[4].equals("null")) {
					line = br.readLine();
					continue;
				}

				String [] index = content[4].split("-");
				if (x._strTableName.equalsIgnoreCase(content[0]) 
						&& content[4].toLowerCase().contains(x._strColumnName.toLowerCase())
						&& content[4].toLowerCase().contains(y._strColumnName.toLowerCase())
						&& content[4].toLowerCase().contains(z._strColumnName.toLowerCase())  ) {

					res[0] = index[0];
					res[1] = index[1];
					res[2] = index[2];
					//					System.out.println(res[0] + res[1] + res[2]);
					flag = true;
					break ;
				}

				line = br.readLine();
			}
			br.close();
			if(flag) {
				order = new SQLTerm[3];
				for(int i1 =0 ; i1 < res.length ; i1++) {
					if(res[i1].equalsIgnoreCase(x._strColumnName)) {
						order[i1] = x ;
						//						System.out.println(res[i1]);
					}
					if(res[i1].equalsIgnoreCase(y._strColumnName)) {
						order[i1] = y ;
						//						System.out.println(res[i1]);
					}
					if(res[i1].equalsIgnoreCase(z._strColumnName)) {
						order[i1] = z ;
						//						System.out.println(res[i1]);
					}
				}
			}

		}catch (IOException e) {
			System.out.println("No table has been created");
		}

		return order ;
	}

	public String[] haveIndex (String tableName , String x , String y , String z) {
		String[] res = new String [3];
		try {
			BufferedReader br = new BufferedReader(new FileReader("metadata.csv"));
			String line = br.readLine();

			int i =0 ;
			while (line != null) {
				String[] content = line.split(",");
				if(content[4].equals("null")) {
					line = br.readLine();
					continue;
				}

				String [] index = content[4].split("-");
				if (tableName.equals(content[0]) && content[4].toLowerCase().contains(x.toLowerCase())
						&& content[4].toLowerCase().contains(y.toLowerCase())
						&& content[4].toLowerCase().contains(z.toLowerCase())  ) {
					res[0] = index[0];
					res[1] = index[1];
					res[2] = index[2];
					break ;
				}

				line = br.readLine();
			}
			br.close();

		}catch (IOException e) {
			System.out.println("No table has been created");
		}
		return res ;
	}

	public void saveIndex(Octree octree , String name) throws IOException {
		FileOutputStream fout = new FileOutputStream(name + ".class");
		ObjectOutputStream oos = new ObjectOutputStream(fout);
		oos.writeObject(octree);
		oos.close();
	}

	public Octree loadIndex(String Indexname) throws IOException {
		// FileInputStream fiut = new FileInputStream(tablename + ".class");
		// ObjectInputStream ois = new ObjectInputStream(fiut);
		try (FileInputStream fiut = new FileInputStream(Indexname + ".class");
				ObjectInputStream ois = new ObjectInputStream(fiut);) {
			return (Octree) ois.readObject();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}

	private static void  insertCoursesRecords(DBApp dbApp, int limit) throws Exception {
        BufferedReader coursesTable = new BufferedReader(new FileReader("C:\\Users\\Asus\\Desktop\\semster 6\\M2\\courses_table.csv"));
        String record;
        Hashtable<String, Object> row = new Hashtable<>();
        int c = limit;
        if (limit == -1) {
            c = 1;
        }
        while ((record = coursesTable.readLine()) != null && c > 0) {
            String[] fields = record.split(",");


            int year = Integer.parseInt(fields[0].trim().substring(0, 4));
            int month = Integer.parseInt(fields[0].trim().substring(5, 7));
            int day = Integer.parseInt(fields[0].trim().substring(8));

            Date dateAdded = new Date(year - 1900, month - 1, day);

            row.put("date_added", dateAdded);

            row.put("course_id", fields[1]);
            row.put("course_name", fields[2]);
            row.put("hours", Integer.parseInt(fields[3]));

            dbApp.insertIntoTable("courses", row);
            row.clear();

            if (limit != -1) {
                c--;
            }
        }

        coursesTable.close();
    }

 private static void  insertStudentRecords(DBApp dbApp, int limit) throws Exception {
        BufferedReader studentsTable = new BufferedReader(new FileReader("C:\\Users\\Asus\\Desktop\\semster 6\\M2\\students_table.csv"));
        String record;
        int c = limit;
        if (limit == -1) {
            c = 1;
        }

        Hashtable<String, Object> row = new Hashtable<>();
        while ((record = studentsTable.readLine()) != null && c > 0) {
            String[] fields = record.split(",");

            row.put("id", fields[0]);
            row.put("first_name", fields[1]);
            row.put("last_name", fields[2]);

//            int year = Integer.parseInt(fields[3].trim().substring(0, 4));
//            int month = Integer.parseInt(fields[3].trim().substring(5, 7));
//            int day = Integer.parseInt(fields[3].trim().substring(8));

//            Date dob = new Date(year - 1900, month - 1, day);
//            row.put("dob", dob);

            double gpa = Double.parseDouble(fields[3].trim());

            row.put("gpa", gpa);

            dbApp.insertIntoTable("students", row);
            row.clear();
            if (limit != -1) {
                c--;
            }
        }
        studentsTable.close();
    }
 private static void insertTranscriptsRecords(DBApp dbApp, int limit) throws Exception {
        BufferedReader transcriptsTable = new BufferedReader(new FileReader("C:\\Users\\Asus\\Desktop\\semster 6\\M2\\transcripts_table.csv"));
        String record;
        Hashtable<String, Object> row = new Hashtable<>();
        int c = limit;
        if (limit == -1) {
            c = 1;
        }
        while ((record = transcriptsTable.readLine()) != null && c > 0) {
            String[] fields = record.split(",");

            row.put("gpa", Double.parseDouble(fields[0].trim()));
            row.put("student_id", fields[1].trim());
            row.put("course_name", fields[2].trim());

            String date = fields[3].trim();
            int year = Integer.parseInt(date.substring(0, 4));
            int month = Integer.parseInt(date.substring(5, 7));
            int day = Integer.parseInt(date.substring(8));

            Date dateUsed = new Date(year - 1900, month - 1, day);
            row.put("date_passed", dateUsed);

            dbApp.insertIntoTable("transcripts", row);
            row.clear();

            if (limit != -1) {
                c--;
            }
        }

        transcriptsTable.close();
    }
 private static void insertPCsRecords(DBApp dbApp, int limit) throws Exception {
        BufferedReader pcsTable = new BufferedReader(new FileReader("C:\\Users\\Asus\\Desktop\\semster 6\\M2\\pcs_table.csv"));
        String record;
        Hashtable<String, Object> row = new Hashtable<>();
        int c = limit;
        if (limit == -1) {
            c = 1;
        }
        while ((record = pcsTable.readLine()) != null && c > 0) {
            String[] fields = record.split(",");

            row.put("pc_id", Integer.parseInt(fields[0].trim()));
            row.put("student_id", fields[1].trim());
            System.out.println(limit);
            dbApp.insertIntoTable("pcs", row);
            row.clear();

            if (limit != -1) {
                c--;
            }
        }

        pcsTable.close();
    }
 private static void createTranscriptsTable(DBApp dbApp) throws Exception {
        // Double CK
        String tableName = "transcripts";

        Hashtable<String, String> htblColNameType = new Hashtable<String, String>();
        htblColNameType.put("gpa", "java.lang.Double");
        htblColNameType.put("student_id", "java.lang.String");
        htblColNameType.put("course_name", "java.lang.String");
        htblColNameType.put("date_passed", "java.util.Date");

        Hashtable<String, String> minValues = new Hashtable<>();
        minValues.put("gpa", "0.7");
        minValues.put("student_id", "43-0000");
        minValues.put("course_name", "AAAAAA");
        minValues.put("date_passed", "1990-01-01");

        Hashtable<String, String> maxValues = new Hashtable<>();
        maxValues.put("gpa", "5.0");
        maxValues.put("student_id", "99-9999");
        maxValues.put("course_name", "zzzzzz");
        maxValues.put("date_passed", "2020-12-31");

        dbApp.createTable(tableName, "gpa", htblColNameType, minValues, maxValues);
    }

    private static void createStudentTable(DBApp dbApp) throws Exception {
        // String CK
        String tableName = "students";

        Hashtable<String, String> htblColNameType = new Hashtable<String, String>();
        htblColNameType.put("id", "java.lang.String");
        htblColNameType.put("first_name", "java.lang.String");
        htblColNameType.put("last_name", "java.lang.String");
      //  htblColNameType.put("dob", "java.util.Date");
        htblColNameType.put("gpa", "java.lang.Double");

        Hashtable<String, String> minValues = new Hashtable<>();
        minValues.put("id", "43-0000");
        minValues.put("first_name", "AAAAAA");
        minValues.put("last_name", "AAAAAA");
   //     minValues.put("dob", "1990-01-01");
        minValues.put("gpa", "0.7");

        Hashtable<String, String> maxValues = new Hashtable<>();
        maxValues.put("id", "99-9999");
        maxValues.put("first_name", "zzzzzz");
        maxValues.put("last_name", "zzzzzz");
  //      maxValues.put("dob", "2000-12-31");
        maxValues.put("gpa", "5.0");

        dbApp.createTable(tableName, "id", htblColNameType, minValues, maxValues);
    }
    private static void createPCsTable(DBApp dbApp) throws Exception {
        // Integer CK
        String tableName = "pcs";

        Hashtable<String, String> htblColNameType = new Hashtable<String, String>();
        htblColNameType.put("pc_id", "java.lang.Integer");
        htblColNameType.put("student_id", "java.lang.String");


        Hashtable<String, String> minValues = new Hashtable<>();
        minValues.put("pc_id", "0");
        minValues.put("student_id", "43-0000");

        Hashtable<String, String> maxValues = new Hashtable<>();
        maxValues.put("pc_id", "20000");
        maxValues.put("student_id", "99-9999");

        dbApp.createTable(tableName, "pc_id", htblColNameType, minValues, maxValues);
    }
    private static void createCoursesTable(DBApp dbApp) throws Exception {
        // Date CK
        String tableName = "courses";

        Hashtable<String, String> htblColNameType = new Hashtable<String, String>();
        htblColNameType.put("date_added", "java.util.Date");
        htblColNameType.put("course_id", "java.lang.String");
        htblColNameType.put("course_name", "java.lang.String");
        htblColNameType.put("hours", "java.lang.Integer");


        Hashtable<String, String> minValues = new Hashtable<>();
        minValues.put("date_added", "1901-01-01");
        minValues.put("course_id", "0000");
        minValues.put("course_name", "AAAAAA");
        minValues.put("hours", "1");

        Hashtable<String, String> maxValues = new Hashtable<>();
        maxValues.put("date_added", "2020-12-31");
        maxValues.put("course_id", "9999");
        maxValues.put("course_name", "zzzzzz");
        maxValues.put("hours", "24");

        dbApp.createTable(tableName, "date_added", htblColNameType, minValues, maxValues);

    }
//    public void testWrongStudentsKeyInsertion() {
//        final DBApp dbApp = new DBApp();
//        dbApp.init();
//
//        String table = "students";
//        Hashtable<String, Object> row = new Hashtable();
//        row.put("id", 123);
//        
//        row.put("first_name", "foo");
//        row.put("last_name", "bar");
//
//        Date dob = new Date(1995 - 1900, 4 - 1, 1);
//        row.put("dob", dob);
//        row.put("gpa", 1.1);
//
//        Assertions.assertThrows(DBAppException.class, () -> {
//                    dbApp.insertIntoTable(table, row);
//                }
//        );
//
//    }
//    public void testExtraTranscriptsInsertion() {
//        final DBApp dbApp = new DBApp();
//        dbApp.init();
//
//        String table = "transcripts";
//        Hashtable<String, Object> row = new Hashtable();
//        row.put("gpa", 1.5);
//        row.put("student_id", "34-9874");
//        row.put("course_name", "bar");
//        row.put("elective", true);
//
//
//        Date date_passed = new Date(2011 - 1900, 4 - 1, 1);
//        row.put("date_passed", date_passed);
//
//
//        Assertions.assertThrows(DBAppException.class, () -> {
//                    dbApp.insertIntoTable(table, row);
//                }
//        );
//    }
  
   /* public static void main(String[] args) throws Exception {
	      DBApp db = new DBApp();
	   //   db.init();

//	        SQLTerm[] arrSQLTerms;
//	        arrSQLTerms = new SQLTerm[2];
//	        arrSQLTerms[0] = new SQLTerm();
//	        arrSQLTerms[0]._strTableName = "students";
//	        arrSQLTerms[0]._strColumnName= "first_name";
//	        arrSQLTerms[0]._strOperator = "=";
//	        arrSQLTerms[0]._objValue =row.get("first_name");
//
//	        arrSQLTerms[1] = new SQLTerm();
//	        arrSQLTerms[1]._strTableName = "students";
//	        arrSQLTerms[1]._strColumnName= "gpa";
//	        arrSQLTerms[1]._strOperator = "<=";
//	        arrSQLTerms[1]._objValue = row.get("gpa");
//
//	        String[]strarrOperators = new String[1];
//	        strarrOperators[0] = "OR";
//	      String table = "students";
//
//	        row.put("first_name", "fooooo");
//	        row.put("last_name", "baaaar");
//
//	        Date dob = new Date(1992 - 1900, 9 - 1, 8);
//	        row.put("dob", dob);
//	        row.put("gpa", 1.1);
//
//	        dbApp.updateTable(table, clusteringKey, row);
//	      createCoursesTable(db);
//	      createPCsTable(db);
//	      createTranscriptsTable(db);
//	      createStudentTable(db);
//	      insertPCsRecords(db,200);
//	      insertTranscriptsRecords(db,200);
//	      insertStudentRecords(db,250);
//	      insertCoursesRecords(db,200);
	      
	      
//	        String tableName = "club";
//
//	        Hashtable<String, String> htblColNameType = new Hashtable<String, String>();
//	        htblColNameType.put("id", "java.lang.Integer");
//	        htblColNameType.put("name", "java.lang.String");
//	        htblColNameType.put("ref", "java.lang.Double");
//	        htblColNameType.put("hours", "java.lang.Integer");


//	        Hashtable<String, String> minValues = new Hashtable<>();
//	        minValues.put("id", "0");
//	        minValues.put("name", "AAAAAA");
//	        minValues.put("ref", "0.0");
//
//	        Hashtable<String, String> maxValues = new Hashtable<>();
//	        maxValues.put("id", "100000");
//	        maxValues.put("name", "zzzzzz");
//	        maxValues.put("ref", "100.0");

	     //   db.createTable(tableName, "id", htblColNameType, minValues, maxValues);

	        Hashtable<String, Object> h = new Hashtable<>();
//	        h.put("id", new String("44-1234"));
	        h.put("first_name", new String("mahy"));
//	        h.put("ref", new Double(0.1));
	        
	       // db.updateTable("students", "44-1234",h);
	        String[] s= {"gpa","id","first_name"};
	        db.createIndex("students", s);
	  }*/
    
    public static void main(String[] args) throws DBAppException, IOException, ParseException {

		DBApp DBApp = new DBApp();
		DBApp.init();

		String strTableName = "Student";
		Hashtable htblColNameType = new Hashtable();
		htblColNameType.put("id", "java.lang.Integer");
		htblColNameType.put("name", "java.lang.String");
		htblColNameType.put("gpa", "java.lang.Double");

		Hashtable htblColNameMin = new Hashtable();
		htblColNameMin.put("id", "0");
		htblColNameMin.put("name", "a");
		htblColNameMin.put("gpa", "0.0");

		Hashtable htblColNameMax = new Hashtable();
		htblColNameMax.put("id", "10000");
		htblColNameMax.put("name", "z");
		htblColNameMax.put("gpa", "100.0");
		System.out.println();
				try {
					DBApp.createTable( strTableName, "id", htblColNameType
							,htblColNameMin,htblColNameMax);
				} catch (DBAppException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
//		 for (int i = 20; i < 30; i++) {
		 //Hashtable htblColNameValue = new Hashtable();
//		  htblColNameValue.clear( );
//		 htblColNameValue.put("id", new Integer(15));
//		 htblColNameValue.put("name", new String("c"));
//		 htblColNameValue.put("gpa", new Double(0.2));
//		 // System.out.print(DBApp.isValid( strTableName , htblColNameValue));
//		 //DBApp.insertIntoTable(strTableName, htblColNameValue);
//		  
//		 }
		// DBApp.updateTable(strTableName,"16", htblColNameValue);

		//		Hashtable htblColNameValue1 = new Hashtable();
		//
		//		htblColNameValue1.put("id", new Integer( 1 ));
		//		htblColNameValue1.put("name", new String("b" ) );
		//		htblColNameValue1.put("Gpa", new Double(0.2));
		//
		//		DBApp.insertIntoTable(strTableName, htblColNameValue1);

				SQLTerm[] arrSQLTerms; 
				arrSQLTerms = new SQLTerm[1]; 
				SQLTerm a = new SQLTerm("a", "", "", "");
				SQLTerm b = new SQLTerm("b", "", "", "");
//				SQLTerm c = new SQLTerm("c", "", "", "");
				arrSQLTerms[0] = a;
//				arrSQLTerms[1] = b;
//				arrSQLTerms[2] = c;
				
//				arrSQLTerms[0]._strTableName = "Student"; 
//				arrSQLTerms[0]._strColumnName= "name"; 
//				arrSQLTerms[0]._strOperator = "="; 
//				arrSQLTerms[0]._objValue = ""; 
				arrSQLTerms[0]._strTableName = "Student"; 
				arrSQLTerms[0]._strColumnName= "gpa"; 
				arrSQLTerms[0]._strOperator = "!="; 
				arrSQLTerms[0]._objValue = new Double( 0.2 ); 
//				arrSQLTerms[2]._strTableName = "Student"; 
//				arrSQLTerms[2]._strColumnName= "id"; 
//				arrSQLTerms[2]._strOperator = "="; 
//				arrSQLTerms[2]._objValue =  new Integer(17); 
				String[] op = {};
				Iterator i = DBApp.selectFromTable(arrSQLTerms, op);
				while(i.hasNext()) {
					System.out.println(i.next());
				}
//						String[] s = {"name","gpa" , "id"} ;
//						try {
//						DBApp.createIndex("Student" , s );
//						}catch(Exception e) {
//							System.out.println(e.getMessage());
//						}

		//	DBApp.updateTable(strTableName, "400", htblColNameValue1);

		//	DBApp.deleteFromTable(strTableName, htblColNameValue1);

		// try{
		// FileOutputStream fout = new FileOutputStream("test" + ".class");
		// ObjectOutputStream oos = new ObjectOutputStream(fout);
		// oos.writeObject(htblColNameType);
		// } catch (Exception ex) {
		// ex.printStackTrace();
		// }
		// try{
		// FileInputStream fiut = new FileInputStream("test" + ".class");
		// ObjectInputStream ois = new ObjectInputStream(fiut);
		// Hashtable h=(Hashtable)ois.readObject();
		// System.out.print(h.get("id"));
		// } catch (Exception ex) {
		// ex.printStackTrace();
		// }

		// DBApp a = new DBApp();
		// DBApp b = new DBApp();
		// System.out.println("ghgh"+a.c);
		// a.c++;
		// System.out.println("jkhj"+b.c);
		// System.out.println("ghgh"+a.c);
	}
	
}
