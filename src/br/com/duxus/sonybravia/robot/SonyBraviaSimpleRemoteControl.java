package br.com.duxus.sonybravia.robot;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 * Classe para representar o controle remoto da TV Sony Bravia
 * @author felipebn
 *
 */
public class SonyBraviaSimpleRemoteControl {
	
	private final static String CODIGO_MENU_HOME = "AAAAAQAAAAEAAABgAw==";
	private final static String CODIGO_UP = "AAAAAQAAAAEAAAB0Aw==";
	private final static String CODIGO_LEFT = "AAAAAQAAAAEAAAA0Aw==";
	private final static String CODIGO_RIGHT = "AAAAAQAAAAEAAAAzAw==";
	private final static String CODIGO_CONFIRM = "AAAAAQAAAAEAAABlAw==";
	private final static String CODIGO_POWEROFF = "AAAAAQAAAAEAAAAvAw==";
	
	
	private String tvIp;
	private String tvMac;
	private CookieStore cookieStore = new BasicCookieStore();
	
	public SonyBraviaSimpleRemoteControl( String tvIp , String tvMac ) {
		this.tvIp = tvIp;
		this.tvMac = tvMac; 
	}
	
	/**
	 * Inicializa o controle da tv verificando se está ligada
	 */
	public void init(){
		if( !this.isPowered() ){
			this.powerOn();
		}
		this.register();
	}
	
	/**
	 * Abre o browser da TV
	 */
	public void openBrowser(){
		sendCommand(CODIGO_MENU_HOME,1000);
		sendCommand(CODIGO_UP , 1000);
		for( int i = 0; i < 5; i++ ){
			sendCommand(CODIGO_LEFT, 500);		
		}
		sendCommand(CODIGO_RIGHT, 500);
		sendCommand(CODIGO_CONFIRM, 500);	
	}
	
	/**
	 * Evnia o comando para desligar a TV
	 */
	public void powerOff() {
		sendCommand(CODIGO_POWEROFF, 1);
	}
	/**
	 * Envia um comando para a TV indicando um tempo de espera para retornar
	 * @param command Código IRCC do comando que deve ser enviado
	 * @param espera Tempo de espera em milisegundos após o envio do comando
	 */
	private void sendCommand(String command, int espera){
		final String url = getUrl("/sony/IRCC");
		String envelope = "<?xml version=\"1.0\"?><s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\"><s:Body><u:X_SendIRCC xmlns:u=\"urn:schemas-sony-com:service:IRCC:1\"><IRCCCode>%s</IRCCCode></u:X_SendIRCC></s:Body></s:Envelope>";
		envelope = String.format( envelope , command );
		try{
			HttpPost request = new HttpPost(url);
			request.setHeader("SOAPACTION", "urn:schemas-sony-com:service:IRCC:1#X_SendIRCC");
			request.setEntity( new StringEntity(envelope) );
			System.out.println( "Enviando comando " + command );
			CloseableHttpResponse response = getHttpClient().execute(request);
			if( response.getStatusLine().getStatusCode() != 200 ){
				throw new Exception("Erro no comando: " + response.getStatusLine() );
			}
			//Sempre espera um segundo para o comando retornar
			try{Thread.sleep(espera);}catch(Exception e){}
		}catch(Exception e){
			e.printStackTrace();
			throw new RuntimeException("Não foi possível enviar o comando para a TV",e);
		}
	}
	
	/**
	 * Faz o registro do controle na TV.
	 * Se já possuir o PIN não será necessária interação do usuário e somente 
	 * será obtido o cookie para realizar as outras operações.
	 */
	public void register(){
		final String payload = "{\"method\":\"actRegister\",\"params\":[{\"clientid\":\"java_pc_remote\",\"nickname\":\"Java PC Remote\",\"level\":\"private\"},[{\"value\":\"yes\",\"function\":\"WOL\"}]],\"id\":8,\"version\":\"1.0\"}";
		final String url = getUrl("/sony/accessControl");
		System.out.println( "Iniciando registro na TV..." );
		try{
			String pin = PinStore.loadPin();
			HttpPost request = new HttpPost(url);
			request.setEntity( new StringEntity(payload) );
			//Se o pin não estiver definido, faz a chamada para registrar e inserir manualmente
			if( pin == null ){
				HttpResponse responseStart = getHttpClient().execute(request);
				String jsonStart = EntityUtils.toString(responseStart.getEntity());
				if( jsonStart.contains("error") && !jsonStart.contains("Unauthorized") ){
					//Lança exception se receber erro e não for Unauthorized
					throw new RuntimeException("Não foi possível iniciar o registro: " + jsonStart );
				}
				//Abre um dialogo para ser inserido o PIN exibido na TV
				pin = inputPin();
				//Armazena o PIN
				PinStore.storePin(pin);
			}
			System.out.println( "Registrando com o pin " + pin );
			
			//Adiciona a informação de autenticação para obter o cookie
			request.addHeader("Authorization", "Basic " + Base64.encodeBase64String((":"+pin).getBytes()) );
			getHttpClient().execute(request);
			System.out.println( "Cookies: " + cookieStore.getCookies() );
			System.out.println( "Registro concluído..." );
		}catch(Exception e){
			e.printStackTrace();
			throw new RuntimeException("Não foi possível registrar o controle na TV",e);
		}
	}

	/**
	 * Verifica se a TV está ligada
	 * @return True se a TV estiver ligada, false caso contrário
	 */
	public boolean isPowered(){
		try{
			final String payload = "{\"id\":2,\"method\":\"getPowerStatus\",\"version\":\"1.0\",\"params\":[]}";
			final String url = getUrl("/sony/system");
			System.out.println( "Verificando status da TV..." );
			HttpPost request = new HttpPost(url);
			request.setEntity( new StringEntity(payload) );
			HttpResponse response = getHttpClient().execute(request);
			String jsonResult = EntityUtils.toString(response.getEntity());
			/*
			 * Pega o primeiro item da lista da propriedade result, como o exemplo de resposta abaixo:
			 * 			{"id":2,"result":[{"status":"standby"}]}
			 */
			JSONObject powerStatus = (JSONObject) ((JSONArray) ((JSONObject) new JSONParser().parse(jsonResult)).get("result")).get(0);
			String status = powerStatus.get("status") == null ? "erro":powerStatus.get("status").toString();
			System.out.println( "Status da TV: " + status );
			
			return "active".equals( status );
		}catch(Exception e){
			e.printStackTrace();
			throw new RuntimeException("Não foi possível verificar o status da TV",e);
		}
	}
	
	/**
	 * Liga a TV através de Wake On Lan (rede)
	 */
	public void powerOn(){
		try{
			System.out.println( String.format( "Ligando a TV (%s)..." , this.tvMac ) );
			WakeOnLan.wake(this.tvMac);
			System.out.println( "Sinal WOL enviado..." );
			System.out.println( "Aguardando a inicialização da TV (20s)...");
			Thread.sleep(20 * 1000);
			System.out.println( "Inicialização concluída!");
		}catch(Exception e){
			e.printStackTrace();
			throw new RuntimeException("Não foi possível iniciar a TV",e);
		}
	}

	private String getUrl( String target ){
		if( target.startsWith("/") ){
			target = target.substring(1);
		}
		return String.format("http://%s/%s", this.tvIp , target );
	}
	
	/**
	 * @return HttpClient configurado com cookies 
	 */
	private CloseableHttpClient getHttpClient(){
		return HttpClientBuilder.create().setDefaultCookieStore(cookieStore).build();
	}
	
	/**
	 * Faz o input do PIN pelo usuário.
	 * @return PIN inserido pelo usuário
	 */
	private String inputPin(){
		return JOptionPane.showInputDialog(new JFrame(), "Digite o PIN exibido na TV:").toString();
	}


}
