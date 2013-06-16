/*******************************************************************************
 * Copyright (c) 2012 Joshua Huelsman.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Joshua Huelsman - Initial implementation.
 *     Zhuowei Zhang - Adapted for PTPatchTool and MCPELauncher.
 *******************************************************************************/
package com.joshuahuelsman.patchtool;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class PTPatch {
	public final static byte[] magic = { (byte) 0xff, 0x50, 0x54, 0x50 };
	public final static byte[] op_codes = { (byte) 0xaa, (byte) 0xdd, (byte) 0xee };
	private byte[] patch_array;
	public int count;
	public String name;
	Header mHeader;
	public PTPatch(){
		mHeader = new Header();
	}
	class Header {
		byte[] magic = new byte[4];
		int minecraft_ver;
		int num_patches;
		byte[] indices;
	}

	public void loadPatch(byte[] patch_array) {
		this.patch_array = patch_array;
		mHeader.minecraft_ver = getMinecraftVersion();
		mHeader.num_patches = getNumPatches();
		mHeader.indices = getIndices();
		count = 0;
	}	
	
	public void loadPatch(File patchf) throws IOException{
		patch_array = new byte[(int) patchf.length()];
		InputStream is = new FileInputStream(patchf);
		is.read(patch_array);
		is.close();
		mHeader.minecraft_ver = getMinecraftVersion();
		mHeader.num_patches = getNumPatches();
		mHeader.indices = getIndices();
		count = 0;
	}
	
	public int getMinecraftVersion(){
		return patch_array[4];
	}
	
	public int getNumPatches(){
		return patch_array[5];
	}
	
	public byte[] getIndices(){
		byte[] ret = new byte[mHeader.num_patches * 4];
		for(int i = 0; i < (mHeader.num_patches * 4); i++){
			ret[i] = patch_array[i + 6];
		}
		return ret;
	}
	
	public boolean checkMagic(){
		if(patch_array[0] == magic[0]){
			if(patch_array[1] == magic[1]){
				if(patch_array[2] == magic[2]){
					if(patch_array[3] == magic[3]){
						return true;
					}
				}
			}
		}
		return false;
	}
	
	public void checkMinecraftVersion(){
		return;
	}
	
	public int getNextAddr(){
		byte[] i = new byte[4];
		i[0] = mHeader.indices[(count*4)];
		i[1] = mHeader.indices[(count*4)+1];
		i[2] = mHeader.indices[(count*4)+2];
		i[3] = mHeader.indices[(count*4)+3];
		
		int index = byteArrayToInt(i);
		
		byte[] b = new byte[4];
		b[0] = patch_array[index];
		b[1] = patch_array[index + 1];
		b[2] = patch_array[index + 2];
		b[3] = patch_array[index + 3];
		return byteArrayToInt(b);
	}
	
	public int getCurrentIndex(){
		byte[] i = new byte[4];
		i[0] = mHeader.indices[(count*4)];
		i[1] = mHeader.indices[(count*4)+1];
		i[2] = mHeader.indices[(count*4)+2];
		i[3] = mHeader.indices[(count*4)+3];
		
		int index = byteArrayToInt(i);
		return index;
	}
	
	public byte[] getNextData(){
		byte[] array = new byte[getDataLength()];
		int index = getCurrentIndex();
		int i;
		int i2 = 0;
		for( i = 0; i < getDataLength(); i++){
			array[i2] = patch_array[i + (index + 4)];
			i2++;
		}
		
		return array;
	}
	
	public int getDataLength(){
		int start = 0;
		int end = 0;
		byte[] i = new byte[4];
		i[0] = mHeader.indices[(count*4)];
		i[1] = mHeader.indices[(count*4)+1];
		i[2] = mHeader.indices[(count*4)+2];
		i[3] = mHeader.indices[(count*4)+3];
		
		if(count != (mHeader.num_patches - 1)){
			byte[] i2 = new byte[4];
			i2[0] = mHeader.indices[((count+1)*4)];
			i2[1] = mHeader.indices[((count+1)*4)+1];
			i2[2] = mHeader.indices[((count+1)*4)+2];
			i2[3] = mHeader.indices[((count+1)*4)+3];
			end = byteArrayToInt(i2);
		}else{
			end = patch_array.length;
		}
		start = (byteArrayToInt(i) + 4);
		
		return (end - start);
		
	}
	
	public void applyPatch(File f) throws IOException{
		
		byte[] barray = new byte[(int) f.length()];
		
		InputStream is = new FileInputStream(f);
		is.read(barray);
		is.close();
		ByteBuffer buf = ByteBuffer.wrap(barray);
		for(count = 0; count < mHeader.num_patches; count++){
			buf.position(getNextAddr());
			buf.put(getNextData());
		}
		
		f.delete();
		OutputStream os = new FileOutputStream(f);
		os.write(buf.array());
		os.close();
	}

	public void applyPatch(byte[] barray) throws IOException{
		
		ByteBuffer buf = ByteBuffer.wrap(barray);
		applyPatch(buf);

	}

	public void applyPatch(ByteBuffer buf) throws IOException{
		for(count = 0; count < mHeader.num_patches; count++){
			buf.position(getNextAddr());
			buf.put(getNextData());
		}

	}

	public void removePatch(ByteBuffer buf, byte[] original) {
		ByteBuffer originalBuf = ByteBuffer.wrap(original);
		for(count = 0; count < mHeader.num_patches; count++){
			int nextAddr = getNextAddr();
			buf.position(nextAddr);
			originalBuf.position(nextAddr);
			byte[] nextData = new byte[getDataLength()];
			originalBuf.get(nextData);
			buf.put(nextData);
		}
	}

	public byte[] getMetaData() {
		count = 0;
		int firstIndex = getCurrentIndex();
		int metaDataStart = (mHeader.num_patches * 4) + 6;
		byte[] retval = new byte[firstIndex - metaDataStart];
		System.arraycopy(patch_array, metaDataStart, retval, 0, retval.length);
		return retval;
	}

	public String getDescription() {
		try {
			byte[] metaData = getMetaData();
			return new String(metaData, "UTF-8");
		} catch (Exception e) {
			e.printStackTrace();
			return ""; //should this return null? feel free to yell at me if it should
		}
	}
	
	public static final byte[] intToByteArray(int value) {
        return new byte[] {
                (byte)(value >>> 24),
                (byte)(value >>> 16),
                (byte)(value >>> 8),
                (byte)value};
	}

	public static final int byteArrayToInt(byte [] b) {
        return (b[0] << 24)
                + ((b[1] & 0xFF) << 16)
                + ((b[2] & 0xFF) << 8)
                + (b[3] & 0xFF);
	}
	
	public static byte[] readPatch(String patch)
			throws IOException {
		File patchf = new File(patch);
		byte[] ret = new byte[(int) patchf.length()];
		InputStream is = new FileInputStream(patch);
		is.read(ret, 0, ret.length);
		is.close();
		return ret;
	}
	
}
