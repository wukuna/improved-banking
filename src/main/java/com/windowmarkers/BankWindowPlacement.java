package com.windowmarkers;

import java.awt.Rectangle;

final class BankWindowPlacement
{
    static final int MIN_WIDTH=488,MIN_HEIGHT=200,MAX_SIZE=4000;
    final Rectangle bounds;
    final int areaWidth,areaHeight;

    BankWindowPlacement(Rectangle bounds,int areaWidth,int areaHeight)
    {
        this.bounds=new Rectangle(bounds);
        this.areaWidth=areaWidth;
        this.areaHeight=areaHeight;
    }

    static BankWindowPlacement read(String value)
    {
        if(value==null || value.length()>100)return null;
        String[] parts=value.split(":");
        if(parts.length!=7 || !"v1".equals(parts[0]))return null;
        try {
            int[] n=new int[6];
            for(int i=0;i<n.length;i++) {
                n[i]=Integer.parseInt(parts[i+1]);
                if(n[i]<0 || n[i]>100000)return null;
            }
            if(n[2]==0 || n[3]==0 || n[4]<n[2] || n[5]<n[3]
                || n[0]>n[4]-n[2] || n[1]>n[5]-n[3])return null;
            return new BankWindowPlacement(new Rectangle(n[0],n[1],n[2],n[3]),n[4],n[5]);
        } catch(NumberFormatException e) {return null;}
    }

    String write()
    {
        return "v1:"+bounds.x+":"+bounds.y+":"+bounds.width+":"+bounds.height+":"+areaWidth+":"+areaHeight;
    }

    Rectangle fit(int availableWidth,int availableHeight,int width,int height)
    {
        int w=limitSize(width,MIN_WIDTH,availableWidth),h=limitSize(height,MIN_HEIGHT,availableHeight);
        return new Rectangle(position(bounds.x,areaWidth-bounds.width,availableWidth-w),
            position(bounds.y,areaHeight-bounds.height,availableHeight-h),w,h);
    }

    static int limitSize(int value,int minimum,int available)
    {
        return Math.max(1,Math.min(available,Math.max(minimum,Math.min(MAX_SIZE,value))));
    }

    private static int position(int saved,int oldSpace,int newSpace)
    {
        if(newSpace<=0)return 0;
        return oldSpace<=0?newSpace/2:(int)Math.round(saved*(double)newSpace/oldSpace);
    }
}
