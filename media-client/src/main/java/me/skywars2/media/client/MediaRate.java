package me.skywars2.media.client;

/** Source frame rate, including 30000/1001; audio sample boundaries do not accumulate rounding drift. */
record MediaRate(int numerator,int denominator) {
    static final MediaRate DEFAULT=new MediaRate(20,1);
    MediaRate {
        if(numerator<1||denominator<1||numerator>1_000_000||denominator>1_000_000
                ||numerator>120L*denominator)throw new IllegalArgumentException("Unsupported media frame rate");
        int a=numerator,b=denominator;while(b!=0){int t=a%b;a=b;b=t;}numerator/=a;denominator/=a;
    }
    static MediaRate parse(String text){
        var parts=text.strip().split("/");
        return new MediaRate(Integer.parseInt(parts[0]),parts.length==1?1:Integer.parseInt(parts[1]));
    }
    long nanos(long frame){return scaled(frame,1_000_000_000L);}
    long samples(long frame){return scaled(frame,48000);}
    int pcmBytes(long frame){return Math.toIntExact((samples(frame+1)-samples(frame))*4);}
    long frameAt(long elapsed){
        long scale=1_000_000_000L*denominator;
        try{return (Math.multiplyExact(elapsed+1,numerator)-1)/scale;}
        catch(ArithmeticException large){return java.math.BigInteger.valueOf(elapsed).add(java.math.BigInteger.ONE)
                .multiply(java.math.BigInteger.valueOf(numerator)).subtract(java.math.BigInteger.ONE)
                .divide(java.math.BigInteger.valueOf(scale)).longValueExact();}
    }
    private long scaled(long frame,long unit){
        long scale=unit*denominator;
        try{return Math.multiplyExact(frame,scale)/numerator;}
        catch(ArithmeticException large){return java.math.BigInteger.valueOf(frame).multiply(java.math.BigInteger.valueOf(scale))
                .divide(java.math.BigInteger.valueOf(numerator)).longValueExact();}
    }
    int framesFor(double seconds){return Math.max(1,(int)Math.ceil(numerator*seconds/denominator));}
    @Override public String toString(){return numerator+"/"+denominator;}
}
