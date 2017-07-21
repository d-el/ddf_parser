/*!****************************************************************************
 * @file		ddf_prser.java
 * @author		Storozhenko Roman
 * @version		V1.0
 * @date		19.07.2017
 * @copyright	GNU Lesser General Public License v3
 * @brief	--
 */

import  java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
 
class ddf_parser{
    public static void main(String[] args) throws IOException  {
    	List<String> headerContent = new ArrayList<String>();
    	List<String> sourceContent = new ArrayList<String>();
    	Peripherals peripherals = new Peripherals();
    	Map<String, Register> registers = new HashMap<String, Register>();
    	
    	if(args.length < 3) {
    		System.out.println(	"Error!\r\n" + 
    							"ddfparser input_ddf output_h output_c\r\n" + 
    							"");
			return;
    	}
    	Path headinputPath = Paths.get(args[0]);
    	Path headerPath = Paths.get(args[1]);
    	Path sourcePath = Paths.get(args[2]);
    	
    	/*!********************************************************************
    	 * Чтение файла
    	 */
    	if(Files.notExists(headinputPath)) {	//Проверяем что файл существует
			System.out.println("Input file is not exist!");
			return;
		}
    	System.out.print("Read file... ");
    	BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(args[0])));
    	List<String> fileContent = new ArrayList<String>();
    	String str;
    	while( (str = br.readLine() ) != null ) {	//читаем все строки
    		fileContent.add(str);
    	}
    	br.close();
    	System.out.println("Done");
    	
    	/*!********************************************************************
    	 * Парсинг
    	 */
    	///Парсим sfr
    	System.out.print("Parse sfr... ");
    	for(int i = 0; i < fileContent.size(); i++) {
    		if(fileContent.get(i).indexOf("sfr = ") != -1) {
    			List<String> found = new ArrayList<String>();
    			
    			Pattern pattern = Pattern.compile("=\\ \\\"([\\w \\(\\)]+)\\\"");
    	    	Matcher matcher = pattern.matcher(fileContent.get(i));
    	    	if(matcher.find()) {	//Найден регистр
    	    		found.add(matcher.group(1));
    	    		
    	    		pattern = Pattern.compile("\\b[0-9]([a-fxA-F0-9 \\(\\)]+){0,}");
    	    		matcher = pattern.matcher(fileContent.get(i));
    	    		while (matcher.find()) {
        	            found.add(matcher.group());
        	        }
    	    		
    	    		Register r = new Register();
    	    		//Заполняем свойства регистра
    	    		r.name = found.get(0);
    	    		r.address = found.get(1);
    	    		r.bytesize = found.get(2);
    	    		//Добавляем регистр в словарь
    	    		registers.put(r.name, r);
    	    	}
    		}
    	}
    	System.out.println("Done");
    	
    	///Парсим группы
    	System.out.print("Parse group... ");
    	for(int i = 0; i < fileContent.size(); i++) {
    		if(fileContent.get(i).indexOf("group = ") != -1) {
    			List<String> found = new ArrayList<String>();
    	    	Pattern pattern = Pattern.compile("\\\"([\\w \\(\\)]+)\\\"");
    	    	Matcher matcher = pattern.matcher(fileContent.get(i));
    	    	
    	    	while (matcher.find()) {
    	            found.add(matcher.group(1));
    	        }
    	    	
    	    	//Peripheral p = new Peripheral();
    	    	peripherals.addPeripheral(found.get(0));
    	    	for(int j = 1; j < found.size(); j++){
    	    		peripherals.addRegister(registers.get(found.get(j)));
    	    	}
    		}
    	}
    	System.out.println("Done");
    	
    	/*!********************************************************************
    	 * Генерация хедер файла
    	 */
    	System.out.print("Generate header file... ");
    	headerContent.add("/*!****************************************************************************\r\n" + 
    			" * @file		" + headerPath.getFileName().toString() + "\r\n" + 
    			" * @author		generated ddfparser\r\n" + 
    			" * @version		V1.0\r\n" + 
    			" * @date		\r\n" + 
    			" * @copyright	GNU Lesser General Public License v3\r\n" + 
    			" * @brief		--\r\n" + 
    			" */\r\n" + 
    			"#ifndef tmsPeriph_H\r\n" + 
    			"#define tmsPeriph_H\r\n" + 
    			" ");
    	
    	headerContent.add("typedef struct{");
    		for(int i = 0; i < peripherals.peripherals.size(); i++) {
    			headerContent.add("    struct{");
    			for(int j = 0; j < peripherals.peripherals.get(i).registers.size(); j++) {
    				headerContent.add("        " 
    								+ "const "
    								+ peripherals.peripherals.get(i).registers.get(j).type
    								+ "\t*"
    								+ peripherals.peripherals.get(i).registers.get(j).name 
    								+ ";");
    			}
    			headerContent.add("    }" + peripherals.peripherals.get(i).formattedName + ";");
				if(i < peripherals.peripherals.size() - 1) {
					headerContent.add("");
				}
    		}
    	headerContent.add("}tmsPeriphPointers_type;");
    	headerContent.add(	"\r\n" +
    						"extern const tmsPeriphPointers_type tmsPeripheral;\r\n" +
    						"#endif //tmsPeriph_H\r\n" + 
    						"/*************** LGPL ************** END OF FILE *********** D_EL ************/\r\n" + 
    						"\r\n" + 
    						"");	
    	System.out.println("Done");
    	
    	/*!********************************************************************
    	 * Генерация с файла
    	 */
    	System.out.print("Generate source file... ");
    	sourceContent.add("/*!****************************************************************************\r\n" + 
    			" * @file		" + sourcePath.getFileName().toString() + "\r\n" + 
    			" * @author		generated ddfparser\r\n" + 
    			" * @version		V1.0\r\n" + 
    			" * @date		\r\n" + 
    			" * @copyright	GNU Lesser General Public License v3\r\n" + 
    			" * @brief		--\r\n" + 
    			" */\r\n" +
    			"#include \"stdio.h\"\r\n" +
    			"#include " + "\"" + headerPath.getFileName().toString() + "\"" +
    			"\r\n"
    			);
    	sourceContent.add("const tmsPeriphPointers_type tmsPeripheral = {");
    	for(int i = 0; i < peripherals.peripherals.size(); i++) {
			//headerContent.add("    struct{");
			for(int j = 0; j < peripherals.peripherals.get(i).registers.size(); j++) {
				sourceContent.add("\t\t" 
								+ "."
								+ peripherals.peripherals.get(i).formattedName
								+ "."
								+ peripherals.peripherals.get(i).registers.get(j).name
								+ " = (void*)"
								+ peripherals.peripherals.get(i).registers.get(j).address
								+ ",");
			}
			//headerContent.add("    }" + peripherals.peripherals.get(i).formattedName + ";");
			if(i < peripherals.peripherals.size()) {
				sourceContent.add("");
			}
		}
    	sourceContent.add("};");
    	sourceContent.add(	"\r\n" +
				"/*************** LGPL ************** END OF FILE *********** D_EL ************/\r\n" + 
				"\r\n" + 
				"");
    	System.out.println("Done");
    	
    	/*!********************************************************************
    	 * Вывод в файлы
    	 */
    	System.out.print("Save out files... ");
		BufferedWriter bhw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(headerPath.toString())));
		for (int j = 0; j < headerContent.size(); j++) {
    		bhw.write(headerContent.get(j));
    		bhw.newLine();
    	}
		bhw.close();
		
		BufferedWriter bcw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(sourcePath.toString())));
		for (int j = 0; j < sourceContent.size(); j++) {
    		bcw.write(sourceContent.get(j));
    		bcw.newLine();
    	}
		bcw.close();
		System.out.println("Done");
    	
    	/*!********************************************************************
    	 * Вывод в консоль
    	 */
    	/*for (int j = 0; j < headerContent.size(); j++) {
    		System.out.println(headerContent.get(j));
    	}*/
    }
}

class Register{
	public String name;
	public String address;
	public String bytesize;
	public String chmod;
	public String type;
}

class Peripheral{
	public String 	name;
	public String 	formattedName;
	List<Register>  registers = new ArrayList<Register>();
}

class Peripherals{
	List<Peripheral> peripherals = new ArrayList<Peripheral>();
	private Peripheral p;
	
	public void addPeripheral(String namePeripheral) {
		p = new Peripheral();
		p.name = namePeripheral;
		p.formattedName = p.name.replaceAll("[ \\(\\)]", "_");
		p.formattedName = p.formattedName.replaceAll("__", "_");
		p.formattedName = p.formattedName.replaceAll("_\\b", "");
		peripherals.add(p);
	}
	
	public void addRegister(Register register) {
		switch(register.bytesize) {
			case "1": 
				register.type = "uint8_t";
				break;
			case "2": 
				register.type = "uint16_t";
				break;
			case "4": 
				register.type = "uint32_t";
				break;
			default: 
				register.type = "uint32_t";
		}
		p.registers.add(register);
	}
}

/*************** LGPL ************** END OF FILE *********** D_EL ************/
