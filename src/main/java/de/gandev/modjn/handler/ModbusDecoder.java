package de.gandev.modjn.handler;

import static de.gandev.modjn.ModbusConstants.MBAP_LENGTH;
import de.gandev.modjn.entity.ModbusFrame;
import de.gandev.modjn.entity.ModbusFunction;
import de.gandev.modjn.entity.ModbusHeader;
import de.gandev.modjn.entity.func.ModbusError;
import de.gandev.modjn.entity.func.request.ReadCoilsRequest;
import de.gandev.modjn.entity.func.response.ReadCoilsResponse;
import de.gandev.modjn.entity.func.request.ReadDiscreteInputsRequest;
import de.gandev.modjn.entity.func.response.ReadDiscreteInputsResponse;
import de.gandev.modjn.entity.func.request.ReadHoldingRegistersRequest;
import de.gandev.modjn.entity.func.response.ReadHoldingRegistersResponse;
import de.gandev.modjn.entity.func.request.ReadInputRegistersRequest;
import de.gandev.modjn.entity.func.response.ReadInputRegistersResponse;
import de.gandev.modjn.entity.func.request.WriteMultipleCoilsRequest;
import de.gandev.modjn.entity.func.response.WriteMultipleCoilsResponse;
import de.gandev.modjn.entity.func.request.WriteMultipleRegistersRequest;
import de.gandev.modjn.entity.func.response.WriteMultipleRegistersResponse;
import de.gandev.modjn.entity.func.WriteSingleCoil;
import de.gandev.modjn.entity.func.WriteSingleRegister;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.internal.RecyclableArrayList;

import java.util.List;

/**
 *
 * @author ag
 */
public class ModbusDecoder extends ByteToMessageDecoder {//ByteToMessageDecoder没有考虑TCP粘包和组包的问题

    private final boolean serverMode;

    public ModbusDecoder(boolean serverMode) {
        this.serverMode = serverMode;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buffer, List<Object> out) {
        if (buffer.capacity() < MBAP_LENGTH + 1 /*Function Code*/) {//只是这个条件是否会有问题？？？？
            return;
        }

        ModbusHeader mbapHeader = ModbusHeader.decode(buffer);//从buffer中读出2+2+2+1个字节组成header

        short functionCode = buffer.readUnsignedByte();//从buffer中独处1个字节，赋值个functionCode

        ModbusFunction function = null;
        switch (functionCode) {              //根据functionCode,来创建对应的ModbusFunction子类实例
            case ModbusFunction.READ_COILS:
                if (serverMode) {            //确定当前是否是服务器模式，boolean serverMode
                    function = new ReadCoilsRequest();   //服务器端需要对request解码-把看不懂的串字符变成看得懂得
                } else {
                    function = new ReadCoilsResponse();   //客户端需要对response解码
                }
                break;
            case ModbusFunction.READ_DISCRETE_INPUTS:
                if (serverMode) {
                    function = new ReadDiscreteInputsRequest();
                } else {
                    function = new ReadDiscreteInputsResponse();
                }
                break;
            case ModbusFunction.READ_INPUT_REGISTERS:
                if (serverMode) {
                    function = new ReadInputRegistersRequest();
                } else {
                    function = new ReadInputRegistersResponse();
                }
                break;
            case ModbusFunction.READ_HOLDING_REGISTERS:
                if (serverMode) {
                    function = new ReadHoldingRegistersRequest();
                } else {
                    function = new ReadHoldingRegistersResponse();
                }
                break;
            case ModbusFunction.WRITE_SINGLE_COIL:
                function = new WriteSingleCoil();
                break;
            case ModbusFunction.WRITE_SINGLE_REGISTER:
                function = new WriteSingleRegister();
                break;
            case ModbusFunction.WRITE_MULTIPLE_COILS:
                if (serverMode) {
                    function = new WriteMultipleCoilsRequest();
                } else {
                    function = new WriteMultipleCoilsResponse();
                }
                break;
            case ModbusFunction.WRITE_MULTIPLE_REGISTERS:
                if (serverMode) {
                    function = new WriteMultipleRegistersRequest();
                } else {
                    function = new WriteMultipleRegistersResponse();
                }
                break;
        }

        if (ModbusFunction.isError(functionCode)) {       //当functionCode是错误类型时，创建ModbusError
            function = new ModbusError(functionCode);
        } else if (function == null) {
            function = new ModbusError(functionCode, (short) 1);
        }

        function.decode(buffer.readBytes(buffer.readableBytes()));//创建好对象的function后读入所有可读的字节，decode是一个抽象方法，被不同子类实现
                                                                  //感觉有问题呀，凭什么是所有可读的字节？答：不同的子类会进行不同的decode，所以是可行的 ？
        ModbusFrame frame = new ModbusFrame(mbapHeader, function);  ////buffer中的一个数据序列被解码成功，成为ModbusFrame

        out.add(frame);      //将ModbusFrame加入到一个list中，这个out对象是list对象，作用是？？？？
    }
}
/*
 * time: 2016.1.4  arthor: haifeng
 * 
 * 在ByteToMessageDecoder（它继承于ChannelInboundhanleradapter）中，decode是被callDecode调用的，而callDecode是在channelRead和channelInactive方法中调用的。
 * 在这两个方法中都定义了一个  RecyclableArrayList “out” = RecyclableArrayList.newInstance();
 * 在channelRead和channelInactive方法最后都调用了out.recycle()
 */