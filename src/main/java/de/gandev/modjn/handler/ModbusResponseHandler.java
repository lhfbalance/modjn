package de.gandev.modjn.handler;

import de.gandev.modjn.ModbusConstants;
import de.gandev.modjn.entity.ModbusFrame;
import de.gandev.modjn.entity.exception.ErrorResponseException;
import de.gandev.modjn.entity.exception.NoResponseException;
import de.gandev.modjn.entity.func.ModbusError;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ares
 */
public abstract class ModbusResponseHandler extends SimpleChannelInboundHandler<ModbusFrame> {

    private static final Logger logger = Logger.getLogger(ModbusResponseHandler.class.getSimpleName());
    private final Map<Integer, ModbusFrame> responses = new HashMap<>(ModbusConstants.TRANSACTION_IDENTIFIER_MAX);

    public ModbusFrame getResponse(int transactionIdentifier)                 //这个方法是同步式使用modbus通信时调用的
            throws NoResponseException, ErrorResponseException {

        long timeoutTime = System.currentTimeMillis() + ModbusConstants.SYNC_RESPONSE_TIMEOUT;  //配置一个modbus响应超时时间
        ModbusFrame frame;
        do {
            frame = responses.get(transactionIdentifier);
        } while (frame == null && (timeoutTime - System.currentTimeMillis()) > 0);  //在这里等待回复，阻塞时长为SYNC_RESPONSE_TIMEOUT;

        if (frame != null) {                              //IO操作还是异步的，只是modbus请求变成了异步，response被放在一个map里，transaID是键，modbusFrame为值
            responses.remove(transactionIdentifier);      //同步式的modbus通信是通过查看map中的数据返回结果的，并不是使用同步式的IO操作              
        }                                                 //异步式的modbus通信就不用使用这个getResponse了，但是还会存到map中

        if (frame == null) {
            throw new NoResponseException();
        } else if (frame.getFunction() instanceof ModbusError) {
            throw new ErrorResponseException((ModbusError) frame.getFunction());
        }

        return frame;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.log(Level.SEVERE, cause.getLocalizedMessage());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ModbusFrame response) throws Exception {
        responses.put(response.getHeader().getTransactionIdentifier(), response);       //把response放到map中
        newResponse(response);        

    }
          //使用异步式的Modbus通信时，复写newResponse方法，在此方法中获取刚刚到达的response，这个方法尽量不要做太多的事情，避免线程耽误时间太久，影响接下来通道的读操作线程
    public abstract void newResponse(ModbusFrame frame);
}
