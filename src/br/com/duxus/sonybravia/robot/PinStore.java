package br.com.duxus.sonybravia.robot;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Classe responsável por armazenar/carregar o PIN utilizado no momento 
 * do registro da aplicação na TV
 * 
 * @author felipebn
 *
 */
public class PinStore {

	private static final String PATH_DIR = System.getProperty("user.home") + "/.javaSonyBraviaRemote";
	private static final String PINSTORE_FILE_NAME = "pinStore";
	
	/**
	 * Armazena o PIN no arquivo PATH_DIR/PINSTORE_FILE_NAME
	 * @param pin
	 */
	public static void storePin( String pin ){
		try{
			
			File dir = new File(PATH_DIR);
			if( !dir.exists() ) dir.mkdir();
			File arquivo = new File(dir.getAbsolutePath() + "/" + PINSTORE_FILE_NAME);
			if( !arquivo.exists() ) arquivo.createNewFile();
			PrintWriter pw = new PrintWriter(arquivo);
			pw.write(pin);
			pw.flush();
			pw.close();
			System.out.println( "Pin armazenado no arquivo " + arquivo.getAbsolutePath() );
		}catch(Exception e){
			e.printStackTrace();
			throw new RuntimeException("Não foi possível armazenar o PIN",e);
		}
	}
	
	/**
	 * @return O PIN armazenado ou NULL se não existir o arquivo ou este estiver vazio
	 */
	public static String loadPin(){
		BufferedReader reader = null;
		try{
			File arquivo = new File(PATH_DIR + "/" + PINSTORE_FILE_NAME);
			if( !arquivo.exists() ) return null;
			reader = new BufferedReader(new FileReader(arquivo));		
			String pin = reader.readLine().trim();
			return pin.isEmpty() ? null:pin;
		}catch(Exception e){
			e.printStackTrace();
			throw new RuntimeException("Não foi possível carregar o PIN armazenado",e);
		}finally{
			if( reader != null ){
				try {
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	
	
}
