package de.gandev.modjn.handler;

import de.gandev.modjn.entity.ModbusFrame;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

/**
 *
 * @author ares
 */
public class ModbusEncoder extends ChannelOutboundHandlerAdapter {

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof ModbusFrame) {
            ModbusFrame frame = (ModbusFrame) msg;
            										//看ChannelHandlerContext这个重要的类
            ctx.writeAndFlush(frame.encode());//对要发送的ModbusFrame进行编码
        } else {
            ctx.write(msg);
        }
    }
}
