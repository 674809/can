package cn.tricheer.canbox;

/**
 * @author by AllenJ on 2018/5/3.
 */

public interface Cmd {
    /**
     * Head Code  0x2E
     * Data Type 0x90
     * Length 2
     * Data0 0x71
     * 0：请求数据帧 0x0280
     * 1：请求数据帧 0x02DE
     * 2：请求数据帧 0x0421
     * 3：请求数据帧 0x05C5
     */
    /**
     * 计算校验位
     *    int a =  (0x24+0x02+0xCF)^0xFF;
     *    Log.i(TAG,"a = " +Integer.toHexString(a));
     */
    int DataType = 0x90;
    int Length =0x02;
    int Data0 = 0x71;
    int Checksum = (DataType +Length +Data0)^0XFF;
    public  String REQUEST_0 = "2E90027100FC";
    public  String REQUEST_1 = "2E90027101FB";
    public  String REQUEST_2 = "2E90027102FA";
    public  String REQUEST_3 = "2E90027103F9";
}
