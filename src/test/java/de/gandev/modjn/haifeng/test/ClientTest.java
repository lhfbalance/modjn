package de.gandev.modjn.haifeng.test;

import de.gandev.modjn.ModbusClient;
import de.gandev.modjn.entity.exception.ConnectionException;
import de.gandev.modjn.entity.exception.ErrorResponseException;
import de.gandev.modjn.entity.exception.NoResponseException;
import de.gandev.modjn.entity.func.response.ReadHoldingRegistersResponse;

public class ClientTest {
	private ModbusClient myModbusClient;
	private String host;
	private int port;

	private ClientTest() throws ConnectionException {
		this.host = "127.0.0.1";
		this.port = 502;
		myModbusClient = new ModbusClient(host, port);
		myModbusClient.setup();
	}
	private ClientTest(String host, int port) throws ConnectionException {
		this.host = host;
		this.port = port;
		myModbusClient = new ModbusClient(host, port);
		myModbusClient.setup();
	}
	public static void main(String[] args) throws ConnectionException {
		ClientTest myClient = new ClientTest();
		
		ReadHoldingRegistersResponse myResponse;
		try {
			myResponse = myClient.myModbusClient.readHoldingRegisters(1, 3);//同步的
			//myResponse = myClient.myModbusClient.readHoldingRegistersAsync(1, 3);//异步
			System.out.println(myResponse.calculateLength());
			System.out.println(myResponse.getRegisters().length);
			for(int i = 0; i < myResponse.getRegisters().length; i++) {
				
				System.out.println(myResponse.getRegisters()[i]);
				
			}
			
		} catch (NoResponseException | ErrorResponseException | ConnectionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			myClient.myModbusClient.close();
		}
	}

}
