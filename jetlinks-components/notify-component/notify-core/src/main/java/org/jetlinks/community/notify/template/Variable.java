package org.jetlinks.community.notify.template;

public interface Variable {

    interface FileFormat {
        String image = "image";
        String video = "video";
        String any = "any";
    }


    /**
     * @see String#format(String, Object...)
     */
    interface NumberFormat {

        //整数
        String integer = "%.0f";
        //1位小数
        String onePlaces = "%.1f";
        //2位小数
        String twoPlaces = "%.2f";

    }

}
