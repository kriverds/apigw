package com.soluvis.ds.apigw.v1.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.soluvis.ds.apigw.v1.application.config.Const;

/**
 * @Class 		: FileUtil
 * @date   		: 2025. 4. 8.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  파일 관련 Util Class
 */
public class FileUtil {
	
	FileUtil() {}
	
	static final Logger logger = LoggerFactory.getLogger(FileUtil.class);
	static final String SEP = File.separator;
	
	/**
	 * @Method		: read
	 * @date   		: 2025. 4. 8.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  File >> String
	 */
	public static String read(String path, String fileName, UUID uuid) {
		logger.info("[{}] FileUtil.read path[{}] fileName[{}]", uuid, path, fileName);
		String strResult = "";
		String fileFullName = path + SEP + fileName;
		try {
			File targetFile = new File(fileFullName);
			strResult = FileUtils.readFileToString(targetFile, Const.NAS_CHARSET);
		} catch (IOException e) {
			CommonUtil.commonException(e, uuid);
			strResult = "";
		}
		logger.info("read file[{}] length[{}]", fileFullName, strResult.length());
		return strResult;
	}
	/**
	 * @Method		: readLines
	 * @date   		: 2025. 4. 8.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  File >> List<String>
	 */
	public static List<String> readLines(String path, String fileName, UUID uuid) {
		logger.info("[{}] FileUtil.readLines path[{}] fileName[{}]", uuid, path, fileName);
		List<String> lines;
		String fileFullName = path + SEP + fileName;
		try {
			File targetFile = new File(fileFullName);
			lines = FileUtils.readLines(targetFile, Const.NAS_CHARSET);
		} catch (IOException e) {
			CommonUtil.commonException(e, uuid);
			lines = new ArrayList<>();
		}
		logger.info("readLines file[{}] size[{}]", fileFullName, lines.size());
		return lines;
	}
	
	public static List<String> readLines(File file, UUID uuid) {
		return readLines(file.getParent(), file.getName(), uuid);
	}
	
	/**
	 * @Method		: write
	 * @date   		: 2025. 4. 8.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  String >> File
	 */
	public static boolean write(String path, String fileName, String message, UUID uuid) {
		logger.info("[{}] FileUtil.write path[{}] fileName[{}] message[{}]", uuid, path, fileName, message);
		boolean bResult;
		String fileFullName = path + SEP + fileName;
		try {
			File directory = new File(path);
			if(!directory.exists()) {
				logger.info("[{}] Make directory: {}", uuid, directory.getName());
				FileUtils.forceMkdir(directory);
			}
			
			File targetFile = new File(fileFullName);
			FileUtils.write(targetFile, message, Const.NAS_CHARSET);
			bResult = true;
		} catch (IOException e) {
			CommonUtil.commonException(e, uuid);
			bResult = false;
		}
		logger.info("write file[{}] result[{}]", fileFullName, bResult);
		return bResult;
	}
	/**
	 * @Method		: writeLines
	 * @date   		: 2025. 4. 8.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  List<String> >> File
	 */
	public static JSONObject writeLines(String path, String fileName, List<String> messageList, UUID uuid) {
		JSONObject jResult = new JSONObject();
		logger.info("[{}] FileUtil.writeLines path[{}] fileName[{}] messageList[{}]", uuid, path, fileName, messageList);
		String fileFullName = path + SEP + fileName;
		try {
			File directory = new File(path);
			if(!directory.exists()) {
				logger.info("[{}] Make directory: {}", uuid, directory.getName());
				FileUtils.forceMkdir(directory);
			}
			
			File targetFile = new File(fileFullName);
			FileUtils.writeLines(targetFile, Const.NAS_CHARSET.toString(), messageList);
			jResult.put(Const.APIGW_KEY_RESULT_CD, Const.APIGW_SUCCESS_CD);
			jResult.put(Const.APIGW_KEY_RESULT_MSG, Const.APIGW_SUCCESS_MSG);
		} catch (IOException e) {
			jResult.put(Const.APIGW_KEY_RESULT_CD, Const.APIGW_FAIL_CD);
			jResult.put(Const.APIGW_KEY_RESULT_MSG, CommonUtil.commonException(e, uuid));
		}
		logger.info("[{}] writeLines file[{}] result[{}]", uuid, fileFullName, jResult.get(Const.APIGW_KEY_RESULT_CD));
		return jResult;
	}
	
	/**
	 * @Method		: delete
	 * @date   		: 2025. 4. 8.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  파일 삭제
	 */
	public static boolean delete(String path, String fileName, UUID uuid) {
		logger.info("[{}] FileUtil.delete path[{}] fileName[{}]", uuid, path, fileName);
		String fileFullName = path + SEP + fileName;
		File targetFile = new File(fileFullName);
		boolean bResult = FileUtils.deleteQuietly(targetFile);
		
		logger.info("[{}] delete file[{}] result[{}]", uuid, targetFile.getName(), bResult);
		
		return bResult;
	}
	
	/**
	 * @Method		: fileMove
	 * @date   		: 2025. 4. 8.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  sourcePath >> targetPath 파일 이동
	 */
	public static JSONObject fileMove(String sourcePath, String targetPath, String fileName, UUID uuid) {
		JSONObject jResult = new JSONObject();
		logger.info("[{}] FileUtil.fileMove sourcePath[{}] targetPath[{}] fileName[{}]", uuid, sourcePath, targetPath, fileName);
		String sourceFileFullName = sourcePath + SEP + fileName;
		String targetFileFullName = targetPath + SEP + fileName;
		File sourceFile = new File(sourceFileFullName);
		File targetFile = new File(targetFileFullName);
		try {
			FileUtils.moveFile(sourceFile, targetFile);
			jResult.put(Const.APIGW_KEY_RESULT_CD, Const.APIGW_SUCCESS_CD);
			jResult.put(Const.APIGW_KEY_RESULT_MSG, Const.APIGW_SUCCESS_MSG);
			logger.info("[{}] FileUtil.fileMove success", uuid);
		} catch (IOException e) {
			jResult.put(Const.APIGW_KEY_RESULT_CD, Const.APIGW_FAIL_CD);
			jResult.put(Const.APIGW_KEY_RESULT_MSG, CommonUtil.commonException(e, uuid));
		}
		
		return jResult;
	}
	/**
	 * @Method		: getFileList
	 * @date   		: 2025. 4. 8.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  파일명*에 해당하는 파일 리스트 조회
	 */
	public static List<File> getFileList(String path, String fileName, UUID uuid){
		logger.info("[{}] FileUtil.getFileList path[{}] fileName[{}]", uuid, path, fileName);
		String asterFileName = fileName+"*";
		WildcardFileFilter filter = new WildcardFileFilter(asterFileName);
		
		return getFileList(path, filter, uuid);
	}
	/**
	 * @Method		: getFileList
	 * @date   		: 2025. 4. 8.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  여러 파일명*에 해당하는 파일 리스트 조회
	 */
	public static List<File> getFileList(String path, List<String> fileNames, UUID uuid){
		logger.info("[{}] FileUtil.getFileList path[{}] fileName[{}]", uuid, path, fileNames);
		List<String> asterFileNames = fileNames.stream().map(s -> s+"*").toList();
		WildcardFileFilter filter = new WildcardFileFilter(asterFileNames);
		
		return getFileList(path, filter, uuid);
	}
	/**
	 * @Method		: getFileList
	 * @date   		: 2025. 4. 8.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  파일필터에 해당되는 파일 조회
	 */
	static List<File> getFileList(String path, WildcardFileFilter filter, UUID uuid){
		File targetDirectory = new File(path);
		Collection<File> files = FileUtils.listFiles(targetDirectory, filter, null);
		logger.info("[{}] files[{}]", uuid, files);
		logger.info("[{}] FileUtil.getFileList size[{}]", uuid, files.size());
		return new ArrayList<>(files);
	}
}