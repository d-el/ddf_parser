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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
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
    	
    	DateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
    	String date = dateFormat.format(new Date());
    	
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
    			
    			//sfr name
    			Pattern pattern = Pattern.compile("=\\ \\\"([\\w \\(\\)]+)\\\"");
    	    	Matcher matcher = pattern.matcher(fileContent.get(i));
    	    	if(matcher.find()) {	//Найден регистр
    	    		Register r = new Register();
    	    		found.add(matcher.group(1));
    	    		//Address, Bytesize
    	    		pattern = Pattern.compile("\\b[0-9]([a-fxA-F0-9 \\(\\)]+){0,}");
    	    		matcher = pattern.matcher(fileContent.get(i));
    	    		while (matcher.find()) {
        	            found.add(matcher.group());
        	        }
    	    		//Заполняем свойства регистра
    	    		r.name = found.get(0);
    	    		r.address = found.get(1);
    	    		r.bytesize = found.get(2);
    	    		
    	    		//Добавляем регистр в словарь
    	    		registers.put(r.name, r);
    	    	}
    	    	
	    		//sfr bit field
	    		List<String> foundbit = new ArrayList<String>();
	    		pattern = Pattern.compile("([\\w.\\(\\)]+)[.]");
	    		matcher = pattern.matcher(fileContent.get(i));
	    		if(matcher.find()) {
	    			String periph = matcher.group(1);
		    		pattern = Pattern.compile("[.]([\\w.\\(\\)]+)\\\"");
		    		matcher = pattern.matcher(fileContent.get(i));
		    		if (matcher.find()) {
		    			foundbit.add(matcher.group(1));
		    			//Bitrange
	    	    		pattern = Pattern.compile("\\b[0-9]([a-fxA-F0-9 \\(\\)]+){0,}");
	    	    		matcher = pattern.matcher(fileContent.get(i));
	    	    		while (matcher.find()) {
	        	            foundbit.add(matcher.group());
	        	        }
		    			
	    	    		registers.get(periph).addBitField(foundbit.get(0), foundbit.get(4), foundbit.get(5));
	    	        }
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
    			" * @date		" + date + "\r\n" + 
    			" * @copyright	GNU Lesser General Public License v3\r\n" + 
    			" */\r\n" + 
    			"#ifndef tmsPeriph_H\r\n" + 
    			"#define tmsPeriph_H\r\n" + 
    			" ");
    	
    	headerContent.add("typedef struct{");
    		for(int i = 0; i < peripherals.peripherals.size(); i++) {
    			headerContent.add("\t" + "struct{");
    			for(int j = 0; j < peripherals.peripherals.get(i).registers.size(); j++) {
    				headerContent.add("\t\t" + "union{");
    				headerContent.add("\t\t\t"
    								+ peripherals.peripherals.get(i).registers.get(j).type
    								+ "\t"
    								+ "all" 
    								+ ";");
    				
    				if(peripherals.peripherals.get(i).registers.get(j).bitField.size() > 0) {
    					headerContent.add("\t\t\t" + "struct{");
    					for(int k = 0; k < peripherals.peripherals.get(i).registers.get(j).bitField.size(); k++) {
            				headerContent.add("\t\t\t\t"
            								+ peripherals.peripherals.get(i).registers.get(j).type
            								+ "\t"
            								+ peripherals.peripherals.get(i).registers.get(j).bitField.get(k).name
            								+ "\t:"
            								+ peripherals.peripherals.get(i).registers.get(j).bitField.get(k).bits
            								+ ";");
            			}
    					headerContent.add("\t\t\t" + "}bit;");
    				}
    				
    				headerContent.add("\t\t}*" + peripherals.peripherals.get(i).registers.get(j).name + ";");
    			}
    			headerContent.add("\t" + "}" + peripherals.peripherals.get(i).formattedName + ";");
				if(i < peripherals.peripherals.size() - 1) {
					headerContent.add("");
				}
    		}
    	headerContent.add("}tmsPeriphPointers_type;");
    	headerContent.add(	"\r\n" +
    						"extern const tmsPeriphPointers_type tmsPeripheral;\r\n" +
    						"\r\n" +
    						"#endif //tmsPeriph_H\r\n" + 
    						"/*************** LGPL ************** END OF FILE *********** D_EL ************/\r\n" + 
    						"\r\n");	
    	System.out.println("Done");
    	
    	/*!********************************************************************
    	 * Генерация с файла
    	 */
    	System.out.print("Generate source file... ");
    	sourceContent.add("/*!****************************************************************************\r\n" + 
    			" * @file		" + sourcePath.getFileName().toString() + "\r\n" + 
    			" * @author		generated ddfparser\r\n" + 
    			" * @date		" + date + "\r\n" + 
    			" * @copyright	GNU Lesser General Public License v3\r\n" + 
    			" */\r\n" +
    			"#include \"stdio.h\"\r\n" +
    			"#include " + "\"" + headerPath.getFileName().toString() + "\"" +
    			"\r\n"
    			);
    	sourceContent.add("const tmsPeriphPointers_type tmsPeripheral = {");
    	for(int i = 0; i < peripherals.peripherals.size(); i++) {
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
			if(i < peripherals.peripherals.size()) {
				sourceContent.add("");
			}
		}
    	sourceContent.add("};");
    	sourceContent.add(	"\r\n" +
				"/*************** LGPL ************** END OF FILE *********** D_EL ************/\r\n" + 
				"\r\n");
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
    	for (int j = 0; j < headerContent.size(); j++) {
    		System.out.println(headerContent.get(j));
    	}
    }
}

class BitField{
	public String name;
	public String positionStart;
	public String positionEnd;
	public String bits;
}

class Register{
	List<BitField> bitField = new ArrayList<BitField>();
	public String name;
	public String address;
	public String bytesize;
	public String chmod;
	public String type;
	private int currentBitPosition;
	
	public Register() {
		currentBitPosition = 0;
	}
	
	public void addBitField(String name, String positionStart, String positionEnd) {
		int bitPosition = Integer.parseInt(positionStart);
		if(bitPosition > (currentBitPosition + 1)) {
			BitField breserv = new BitField();
			breserv.name = "reserv" + Integer.toString(currentBitPosition);
			breserv.bits = Integer.toString(bitPosition - currentBitPosition);
			bitField.add(breserv);
		}
		
		BitField b = new BitField();
		b.name = name;
		b.positionStart = positionStart;
		b.positionEnd = positionEnd;
		int numbits = Integer.parseInt(b.positionEnd) - Integer.parseInt(b.positionStart) + 1;
		b.bits = Integer.toString(numbits);
		
		currentBitPosition = bitPosition + numbits;
		bitField.add(b);
	}
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
