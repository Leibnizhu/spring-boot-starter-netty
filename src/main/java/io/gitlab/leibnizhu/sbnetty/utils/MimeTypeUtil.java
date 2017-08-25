package io.gitlab.leibnizhu.sbnetty.utils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * @author Leibniz.Hu
 * Created on 2017-08-24 21:03.
 */
public class MimeTypeUtil {
    private static final Map<String, String> MIME_TYPE_MAPPING = new HashMap<>();

    static {
        MIME_TYPE_MAPPING.put("abs", "audio/x-mpeg");
        MIME_TYPE_MAPPING.put("ai", "application/postscript");
        MIME_TYPE_MAPPING.put("aif", "audio/x-aiff");
        MIME_TYPE_MAPPING.put("aifc", "audio/x-aiff");
        MIME_TYPE_MAPPING.put("aiff", "audio/x-aiff");
        MIME_TYPE_MAPPING.put("aim", "application/x-aim");
        MIME_TYPE_MAPPING.put("art", "image/x-jg");
        MIME_TYPE_MAPPING.put("asf", "video/x-ms-asf");
        MIME_TYPE_MAPPING.put("asx", "video/x-ms-asf");
        MIME_TYPE_MAPPING.put("au", "audio/basic");
        MIME_TYPE_MAPPING.put("avi", "video/x-msvideo");
        MIME_TYPE_MAPPING.put("avx", "video/x-rad-screenplay");
        MIME_TYPE_MAPPING.put("bcpio", "application/x-bcpio");
        MIME_TYPE_MAPPING.put("bin", "application/octet-stream");
        MIME_TYPE_MAPPING.put("bmp", "image/bmp");
        MIME_TYPE_MAPPING.put("body", "text/html");
        MIME_TYPE_MAPPING.put("cdf", "application/x-cdf");
        MIME_TYPE_MAPPING.put("cer", "application/pkix-cert");
        MIME_TYPE_MAPPING.put("class", "application/java");
        MIME_TYPE_MAPPING.put("cpio", "application/x-cpio");
        MIME_TYPE_MAPPING.put("csh", "application/x-csh");
        MIME_TYPE_MAPPING.put("css", "text/css");
        MIME_TYPE_MAPPING.put("dib", "image/bmp");
        MIME_TYPE_MAPPING.put("doc", "application/msword");
        MIME_TYPE_MAPPING.put("dtd", "application/xml-dtd");
        MIME_TYPE_MAPPING.put("dv", "video/x-dv");
        MIME_TYPE_MAPPING.put("dvi", "application/x-dvi");
        MIME_TYPE_MAPPING.put("eps", "application/postscript");
        MIME_TYPE_MAPPING.put("etx", "text/x-setext");
        MIME_TYPE_MAPPING.put("exe", "application/octet-stream");
        MIME_TYPE_MAPPING.put("gif", "image/gif");
        MIME_TYPE_MAPPING.put("gtar", "application/x-gtar");
        MIME_TYPE_MAPPING.put("gz", "application/x-gzip");
        MIME_TYPE_MAPPING.put("hdf", "application/x-hdf");
        MIME_TYPE_MAPPING.put("hqx", "application/mac-binhex40");
        MIME_TYPE_MAPPING.put("htc", "text/x-component");
        MIME_TYPE_MAPPING.put("htm", "text/html");
        MIME_TYPE_MAPPING.put("html", "text/html");
        MIME_TYPE_MAPPING.put("ief", "image/ief");
        MIME_TYPE_MAPPING.put("jad", "text/vnd.sun.j2me.app-descriptor");
        MIME_TYPE_MAPPING.put("jar", "application/java-archive");
        MIME_TYPE_MAPPING.put("java", "text/x-java-source");
        MIME_TYPE_MAPPING.put("jnlp", "application/x-java-jnlp-file");
        MIME_TYPE_MAPPING.put("jpe", "image/jpeg");
        MIME_TYPE_MAPPING.put("jpeg", "image/jpeg");
        MIME_TYPE_MAPPING.put("jpg", "image/jpeg");
        MIME_TYPE_MAPPING.put("js", "application/javascript");
        MIME_TYPE_MAPPING.put("jsf", "text/plain");
        MIME_TYPE_MAPPING.put("jspf", "text/plain");
        MIME_TYPE_MAPPING.put("kar", "audio/midi");
        MIME_TYPE_MAPPING.put("latex", "application/x-latex");
        MIME_TYPE_MAPPING.put("m3u", "audio/x-mpegurl");
        MIME_TYPE_MAPPING.put("mac", "image/x-macpaint");
        MIME_TYPE_MAPPING.put("man", "text/troff");
        MIME_TYPE_MAPPING.put("mathml", "application/mathml+xml");
        MIME_TYPE_MAPPING.put("me", "text/troff");
        MIME_TYPE_MAPPING.put("mid", "audio/midi");
        MIME_TYPE_MAPPING.put("midi", "audio/midi");
        MIME_TYPE_MAPPING.put("mif", "application/x-mif");
        MIME_TYPE_MAPPING.put("mov", "video/quicktime");
        MIME_TYPE_MAPPING.put("movie", "video/x-sgi-movie");
        MIME_TYPE_MAPPING.put("mp1", "audio/mpeg");
        MIME_TYPE_MAPPING.put("mp2", "audio/mpeg");
        MIME_TYPE_MAPPING.put("mp3", "audio/mpeg");
        MIME_TYPE_MAPPING.put("mp4", "video/mp4");
        MIME_TYPE_MAPPING.put("mpa", "audio/mpeg");
        MIME_TYPE_MAPPING.put("mpe", "video/mpeg");
        MIME_TYPE_MAPPING.put("mpeg", "video/mpeg");
        MIME_TYPE_MAPPING.put("mpega", "audio/x-mpeg");
        MIME_TYPE_MAPPING.put("mpg", "video/mpeg");
        MIME_TYPE_MAPPING.put("mpv2", "video/mpeg2");
        MIME_TYPE_MAPPING.put("nc", "application/x-netcdf");
        MIME_TYPE_MAPPING.put("oda", "application/oda");
        MIME_TYPE_MAPPING.put("odb", "application/vnd.oasis.opendocument.database");
        MIME_TYPE_MAPPING.put("odc", "application/vnd.oasis.opendocument.chart");
        MIME_TYPE_MAPPING.put("odf", "application/vnd.oasis.opendocument.formula");
        MIME_TYPE_MAPPING.put("odg", "application/vnd.oasis.opendocument.graphics");
        MIME_TYPE_MAPPING.put("odi", "application/vnd.oasis.opendocument.image");
        MIME_TYPE_MAPPING.put("odm", "application/vnd.oasis.opendocument.text-master");
        MIME_TYPE_MAPPING.put("odp", "application/vnd.oasis.opendocument.presentation");
        MIME_TYPE_MAPPING.put("ods", "application/vnd.oasis.opendocument.spreadsheet");
        MIME_TYPE_MAPPING.put("odt", "application/vnd.oasis.opendocument.text");
        MIME_TYPE_MAPPING.put("otg", "application/vnd.oasis.opendocument.graphics-template");
        MIME_TYPE_MAPPING.put("oth", "application/vnd.oasis.opendocument.text-web");
        MIME_TYPE_MAPPING.put("otp", "application/vnd.oasis.opendocument.presentation-template");
        MIME_TYPE_MAPPING.put("ots", "application/vnd.oasis.opendocument.spreadsheet-template ");
        MIME_TYPE_MAPPING.put("ott", "application/vnd.oasis.opendocument.text-template");
        MIME_TYPE_MAPPING.put("ogx", "application/ogg");
        MIME_TYPE_MAPPING.put("ogv", "video/ogg");
        MIME_TYPE_MAPPING.put("oga", "audio/ogg");
        MIME_TYPE_MAPPING.put("ogg", "audio/ogg");
        MIME_TYPE_MAPPING.put("spx", "audio/ogg");
        MIME_TYPE_MAPPING.put("flac", "audio/flac");
        MIME_TYPE_MAPPING.put("anx", "application/annodex");
        MIME_TYPE_MAPPING.put("axa", "audio/annodex");
        MIME_TYPE_MAPPING.put("axv", "video/annodex");
        MIME_TYPE_MAPPING.put("xspf", "application/xspf+xml");
        MIME_TYPE_MAPPING.put("pbm", "image/x-portable-bitmap");
        MIME_TYPE_MAPPING.put("pct", "image/pict");
        MIME_TYPE_MAPPING.put("pdf", "application/pdf");
        MIME_TYPE_MAPPING.put("pgm", "image/x-portable-graymap");
        MIME_TYPE_MAPPING.put("pic", "image/pict");
        MIME_TYPE_MAPPING.put("pict", "image/pict");
        MIME_TYPE_MAPPING.put("pls", "audio/x-scpls");
        MIME_TYPE_MAPPING.put("png", "image/png");
        MIME_TYPE_MAPPING.put("pnm", "image/x-portable-anymap");
        MIME_TYPE_MAPPING.put("pnt", "image/x-macpaint");
        MIME_TYPE_MAPPING.put("ppm", "image/x-portable-pixmap");
        MIME_TYPE_MAPPING.put("ppt", "application/vnd.ms-powerpoint");
        MIME_TYPE_MAPPING.put("pps", "application/vnd.ms-powerpoint");
        MIME_TYPE_MAPPING.put("ps", "application/postscript");
        MIME_TYPE_MAPPING.put("psd", "image/vnd.adobe.photoshop");
        MIME_TYPE_MAPPING.put("qt", "video/quicktime");
        MIME_TYPE_MAPPING.put("qti", "image/x-quicktime");
        MIME_TYPE_MAPPING.put("qtif", "image/x-quicktime");
        MIME_TYPE_MAPPING.put("ras", "image/x-cmu-raster");
        MIME_TYPE_MAPPING.put("rdf", "application/rdf+xml");
        MIME_TYPE_MAPPING.put("rgb", "image/x-rgb");
        MIME_TYPE_MAPPING.put("rm", "application/vnd.rn-realmedia");
        MIME_TYPE_MAPPING.put("roff", "text/troff");
        MIME_TYPE_MAPPING.put("rtf", "application/rtf");
        MIME_TYPE_MAPPING.put("rtx", "text/richtext");
        MIME_TYPE_MAPPING.put("sh", "application/x-sh");
        MIME_TYPE_MAPPING.put("shar", "application/x-shar");
        /*"shtml", "text/x-server-parsed-html",*/
        MIME_TYPE_MAPPING.put("sit", "application/x-stuffit");
        MIME_TYPE_MAPPING.put("snd", "audio/basic");
        MIME_TYPE_MAPPING.put("src", "application/x-wais-source");
        MIME_TYPE_MAPPING.put("sv4cpio", "application/x-sv4cpio");
        MIME_TYPE_MAPPING.put("sv4crc", "application/x-sv4crc");
        MIME_TYPE_MAPPING.put("svg", "image/svg+xml");
        MIME_TYPE_MAPPING.put("svgz", "image/svg+xml");
        MIME_TYPE_MAPPING.put("swf", "application/x-shockwave-flash");
        MIME_TYPE_MAPPING.put("t", "text/troff");
        MIME_TYPE_MAPPING.put("tar", "application/x-tar");
        MIME_TYPE_MAPPING.put("tcl", "application/x-tcl");
        MIME_TYPE_MAPPING.put("tex", "application/x-tex");
        MIME_TYPE_MAPPING.put("texi", "application/x-texinfo");
        MIME_TYPE_MAPPING.put("texinfo", "application/x-texinfo");
        MIME_TYPE_MAPPING.put("tif", "image/tiff");
        MIME_TYPE_MAPPING.put("tiff", "image/tiff");
        MIME_TYPE_MAPPING.put("tr", "text/troff");
        MIME_TYPE_MAPPING.put("tsv", "text/tab-separated-values");
        MIME_TYPE_MAPPING.put("txt", "text/plain");
        MIME_TYPE_MAPPING.put("ulw", "audio/basic");
        MIME_TYPE_MAPPING.put("ustar", "application/x-ustar");
        MIME_TYPE_MAPPING.put("vxml", "application/voicexml+xml");
        MIME_TYPE_MAPPING.put("xbm", "image/x-xbitmap");
        MIME_TYPE_MAPPING.put("xht", "application/xhtml+xml");
        MIME_TYPE_MAPPING.put("xhtml", "application/xhtml+xml");
        MIME_TYPE_MAPPING.put("xls", "application/vnd.ms-excel");
        MIME_TYPE_MAPPING.put("xml", "application/xml");
        MIME_TYPE_MAPPING.put("xpm", "image/x-xpixmap");
        MIME_TYPE_MAPPING.put("xsl", "application/xml");
        MIME_TYPE_MAPPING.put("xslt", "application/xslt+xml");
        MIME_TYPE_MAPPING.put("xul", "application/vnd.mozilla.xul+xml");
        MIME_TYPE_MAPPING.put("xwd", "image/x-xwindowdump");
        MIME_TYPE_MAPPING.put("vsd", "application/vnd.visio");
        MIME_TYPE_MAPPING.put("wav", "audio/x-wav");
        MIME_TYPE_MAPPING.put("wbmp", "image/vnd.wap.wbmp");
        MIME_TYPE_MAPPING.put("wml", "text/vnd.wap.wml");
        MIME_TYPE_MAPPING.put("wmlc", "application/vnd.wap.wmlc");
        MIME_TYPE_MAPPING.put("wmls", "text/vnd.wap.wmlsc");
        MIME_TYPE_MAPPING.put("wmlscriptc", "application/vnd.wap.wmlscriptc");
        MIME_TYPE_MAPPING.put("wmv", "video/x-ms-wmv");
        MIME_TYPE_MAPPING.put("wrl", "model/vrml");
        MIME_TYPE_MAPPING.put("wspolicy", "application/wspolicy+xml");
        MIME_TYPE_MAPPING.put("Z", "application/x-compress");
        MIME_TYPE_MAPPING.put("z", "application/x-compress");
        MIME_TYPE_MAPPING.put("zip", "application/zip");
    }

    public static String getMimeTypeByFileName(String fileName){
        if (fileName == null)
            return (null);
        int period = fileName.lastIndexOf('.');
        if (period < 0)
            return (null);
        String extension = fileName.substring(period + 1);
        if (extension.length() < 1)
            return (null);
        return getMimeTypeByExtension(extension);
    }

    private static String getMimeTypeByExtension(String extension){
        return MIME_TYPE_MAPPING.get(extension.toLowerCase(Locale.ENGLISH));
    }
}
