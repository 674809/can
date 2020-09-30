// IBackService.aidl
package cn.tricheer.canbox;
import cn.tricheer.canbox.IChangeCallBack;
// Declare any non-default types here with import statements

interface IBackService {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
   //aidl接口，用于线程通讯
   	String  onVinChange();
   	//----注册监听----//
     void registerListener(IChangeCallBack callBack);
     void unRegisterListener(IChangeCallBack callBack);

       //-----------//
}
