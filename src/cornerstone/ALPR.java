package cornerstone;

public class ALPR {
//    static {
//        // Load the OpenALPR library at runtime
//        // openalprjni.dll (Windows) or libopenalprjni.so (Linux/Mac)
////        System.loadLibrary("libopenalprjni");
//    	/*try{
//            System.out.println("Trying to load: libopenalprjni");
//            System.loadLibrary("libopenalprjni");
//        }catch (Throwable e){
//            System.out.println("Failed: "+e.getMessage());
//        }
//        System.out.println("Success");*/
//    	System.load("/usr/lib/libopenalprjni.so");
//    }

    private native void initialize(String country, String configFile, String runtimeDir);
    private native void dispose();

    private native boolean is_loaded();				
    private native String native_recognize(String imageFile);
    private native String native_recognize(byte[] imageBytes);

    private native void set_default_region(String region);
    private native void detect_region(boolean detectRegion);
    private native void set_top_n(int topN);
    private native String get_version();

    public ALPR(String country, String configFile, String runtimeDir) {
//        initialize(country, configFile, runtimeDir);
    }

    public void unload() {
        dispose();
    }

    public boolean isLoaded() {
        return is_loaded();
    }

    public String recognize(String imageFile) {
    	return native_recognize(imageFile);
    }


    public String recognize(byte[] imageBytes) {
//        return native_recognize(imageBytes);
    	return "";
    }


    public void setTopN(int topN) {
        set_top_n(topN);
    }

    public void setDefaultRegion(String region) {
        set_default_region(region);
    }

    public void setDetectRegion(boolean detectRegion) {
        detect_region(detectRegion);
    }

    public String getVersion() {
        return get_version();
    }
}
