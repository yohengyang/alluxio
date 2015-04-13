/*
 * Licensed to the University of California, Berkeley under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package tachyon.web;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.ByteStreams;

import tachyon.Constants;
import tachyon.TachyonURI;
import tachyon.conf.TachyonConf;

/**
 * Servlet for downloading a local file
 */
public class WebInterfaceDownloadLocalServlet extends HttpServlet {
  private static final long serialVersionUID = 7260819317567193560L;

  private static final Logger LOG = LoggerFactory.getLogger(Constants.LOGGER_TYPE);

  private final transient TachyonConf mTachyonConf;

  public WebInterfaceDownloadLocalServlet() {
    mTachyonConf = new TachyonConf();
  }

  /**
   * Prepares for downloading a file
   * 
   * @param request The HttpServletRequest object
   * @param response The HttpServletResponse object
   * @throws ServletException
   * @throws IOException
   */
  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    String requestPath = request.getParameter("path");
    if (requestPath == null || requestPath.isEmpty()) {
      requestPath = TachyonURI.SEPARATOR;
    }

    // Download a file from the local filesystem.
    String baseDir = mTachyonConf.get(Constants.TACHYON_HOME, Constants.DEFAULT_HOME);

    // Only allow filenames as the path, to avoid downloading arbitrary local files.
    requestPath = Paths.get(requestPath).getFileName().toString();
    Path logFilePath = Paths.get(baseDir, "/logs", "/" + requestPath);
    try {
      downloadLogFile(logFilePath, request, response);
    } catch (NoSuchFileException nsfe) {
      request.setAttribute("invalidPathError", "Error: Invalid file " + nsfe.getMessage());
      request.setAttribute("currentPath", requestPath);
      request.setAttribute("downloadLogFile", 1);
      request.setAttribute("viewingOffset", 0);
      request.setAttribute("baseUrl", "./browseLogs");
      getServletContext().getRequestDispatcher("/viewFile.jsp").forward(request, response);
    }
  }

  /**
   * This function prepares for downloading a log file on the local filesystem.
   *
   * @param path The path of the local log file to download
   * @param request The HttpServletRequest object
   * @param response The HttpServletResponse object
   * @throws IOException
   */
  private void downloadLogFile(Path path, HttpServletRequest request,
                               HttpServletResponse response) throws IOException {
    long len = Files.size(path);
    InputStream is = Files.newInputStream(path);
    String fileName = path.getFileName().toString();
    response.setContentType("application/octet-stream");
    if (len <= Integer.MAX_VALUE) {
      response.setContentLength((int) len);
    } else {
      response.addHeader("Content-Length", Long.toString(len));
    }
    response.addHeader("Content-Disposition", "attachment;filename=" + fileName);

    ServletOutputStream out = null;
    try {
      out = response.getOutputStream();
      ByteStreams.copy(is, out);
    } finally {
      if (out != null) {
        out.flush();
        out.close();
      }
      if (is != null) {
        is.close();
      }
    }
  }
}
