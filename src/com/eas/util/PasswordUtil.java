package com.eas.util;
import java.nio.charset.StandardCharsets; import java.security.MessageDigest;
public final class PasswordUtil { private PasswordUtil(){} public static String hash(String value){ try{ MessageDigest md=MessageDigest.getInstance("SHA-256"); byte[] data=md.digest(value.getBytes(StandardCharsets.UTF_8)); StringBuilder b=new StringBuilder(); for(byte x:data)b.append(String.format("%02x",x)); return b.toString(); }catch(Exception ex){throw new IllegalStateException(ex);} } }
