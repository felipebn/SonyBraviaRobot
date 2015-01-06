package br.com.duxus.sonybravia.robot;

import java.util.Arrays;

/**
 * Main para execução do programa via console
 * 
 * Uso: java -jar sonybraviaremote.jar -ip [IP_DA_TV] -mac [MAC_ADDRESS_DA_TV] -[iniciar|desligar]
 * 
 * 
 * @author felipebn
 *
 */
public class MainConsole {

	private final static String ACAO_INICIAR = "iniciar";
	private final static String ACAO_DESLIGAR = "desligar";
	
	public static void main(String[] args){
		//Processa os parâmetros
		if( args.length != 5 ){
			die("Argumentos inválidos!\nUso: java -jar sonybraviaremote.jar -ip [IP_DA_TV] -mac [MAC_ADDRESS_DA_TV] -[iniciar|desligar]");
		}
		
		String ip = null,mac = null,acao = null;
		for( int i = 0; i < args.length ; i++ ){
			String a = args[i];
			if( a.equals("-ip") ) ip = args[++i];
			else if( a.equals("-mac") ) mac = args[++i];
			else if( a.endsWith(ACAO_INICIAR) || a.endsWith(ACAO_DESLIGAR) ) acao = a.substring(1);
		}		
		
		if( !Arrays.asList(ACAO_INICIAR,ACAO_DESLIGAR).contains(acao) ){
			die(String.format( "Ação '%s' é inválida" , acao));
		}
		
		//Inicializa o controle
		SonyBraviaSimpleRemoteControl remote = new SonyBraviaSimpleRemoteControl( ip , mac );
		remote.init();
		
		//Executa a ação solicitada
		if( acao.equals(ACAO_INICIAR) ){
			remote.openBrowser();	
		}else if( acao.equals(ACAO_DESLIGAR) ){
			remote.powerOff();	
		}
	}
	
	
	
	private static final void die( String msg ){
		System.err.println(msg);
		System.exit(-1);
	}
}
