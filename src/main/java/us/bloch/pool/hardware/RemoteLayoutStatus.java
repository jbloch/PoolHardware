package us.bloch.pool.hardware;

import java.util.Arrays;

import static us.bloch.pool.hardware.PentairBusCodes.Operation.*;

/** Message sent by the pool controller to describe the QuickTouch remote layout. */
public class RemoteLayoutStatus extends Message {

    final PentairBus.Circuit[] remoteRow = new PentairBus.Circuit[4];

    RemoteLayoutStatus(byte ver, byte dst, byte src, byte cmd, byte[] data) {
        super(ver, dst, src, cmd, data);
        assert cmd == RLS;

        for(int i = 0; i < 4; i++)
            remoteRow[i] = PentairBus.Circuit.forCode(data[i]);
    }

    public String toString() {
        return "Remote rows: " + Arrays.toString(remoteRow);
    }
}
