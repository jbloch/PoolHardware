package us.bloch.pool.hardware;

/**
 * Hexadecimal constants that comprise the "nouns and verbs" of the byte codes on the Pentair bus.
 * The names of these constants are essentially mnemonics for the language spoken on the
 * Pentair bus. These mnemonics are our own, unrelated to any mnemonics that Pentair may use
 * internally. This class (and its nested classes) are noninstantiable. They are merely
 * containers for numerical constants.
 */
class PentairBusCodes {
    /**
     * The protocol number that occupies the first (post-header) byte of each message. The
     * meaning of the opcode (the fourth byte) varies depending on the protocol.
     */
    static class Protocol {
        /** This is the "common" protocol, used by devices other than the IntelliFlo/Pro pump. */
        static final byte CP = (byte) 0x01;

        /** This is the pump protocol, used by all messages to and from the IntelliFlo/Pro pump. */
        static final byte PP = (byte) 0x00;
    }

    /**
     * The "station" codes that occupy the second and third (post-header) bytes of each message,
     * which represent the source and destination of the message.
     */
    static class Station {
        /** The controller (e.g., Intellitouch, SunTouch) */
        static final byte CTL = (byte) 0x10;

        /** Basic remote transceiver (e.g., QuickTouch) */
        static final byte RQT = (byte) 0x48;

        /** Fancy remote transceiver (e.g., ScreenLogic) */
        static final byte RSL = (byte) 0x22;

        /** The pump (IntelliFlo / IntelliPro). In a multipump system, pumps are 0x60 - 0x6f */
        static final byte PMP = (byte) 0x60;

        /** A broadcast message (i.e., "all interested recipients") */
        static final byte BDC = (byte) 0xc8;

        /** Returns true if the given station code represents a pump. */
        static boolean isPump(byte station) { return (station & 0xf0) == 0x60; }
    }

    /**
     * The "opcodes" that occupy the fourth (post-header) byte of each message, and describe its
     * function.
     * todo described letter conventions
     */
    static class Operation {
        /*
         *These opcodes are used with protocol COM.
         */

        /** SystemStatus */
        static final byte SYS = (byte) 0x02;

        /** ClockStatus */
        static final byte CLS = (byte) 0x05;

        /** ClockChangeRequest */
        static final byte CLC = (byte) 0x85;

        /** HeatStatus */
        static final byte HTS = (byte) 0x08;

        /** HeatStatusQuery */
        static final byte HSQ = (byte) 0xc8;

        /** HeatConfigurationChangeRequest */
        static final byte HCC = (byte) 0x88;

        /** RemoteLayoutStatus */
        static final byte RLS = (byte) 0x21;

        /** RemoteLayoutQuery */
        static final byte RLQ = (byte) 0xe1;

        /** CircuitStateChangeRequest */
        static final byte CSC = (byte) 0x86;

        /** StateChangeResponse */
        static final byte SCR = (byte) 0x01;

        /*
         * These opcodes are used with protocol PMP
         */

        /** PumpSpeedMessage (The R in the mnemonic stands for RPM) */
        static final byte PRM = (byte) 0x01;

        /** PumpControlRegimenMessage */
        static final byte PCM = (byte) 0x04;

        /** PumpPowerStateMessage */
        static final byte PPM = (byte) 0x06;

        /** PumpStatusRequest or PumpStatus, depending on recipient */
        static final byte PSM = (byte) 0x07;
    }
}
