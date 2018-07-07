/*
 * This file is part of dependency-check-core.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright (c) 2018 Paul Irwin. All Rights Reserved.
 */
package org.owasp.dependencycheck.analyzer;

import org.owasp.dependencycheck.Engine;
import org.owasp.dependencycheck.analyzer.exception.AnalysisException;
import org.owasp.dependencycheck.data.nuget.MSBuildProjectParseException;
import org.owasp.dependencycheck.data.nuget.MSBuildProjectParser;
import org.owasp.dependencycheck.data.nuget.NugetPackageReference;
import org.owasp.dependencycheck.data.nuget.XPathMSBuildProjectParser;
import org.owasp.dependencycheck.dependency.Confidence;
import org.owasp.dependencycheck.dependency.Dependency;
import org.owasp.dependencycheck.dependency.EvidenceType;
import org.owasp.dependencycheck.exception.InitializationException;
import org.owasp.dependencycheck.utils.FileFilterBuilder;
import org.owasp.dependencycheck.utils.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.ThreadSafe;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;

import static org.owasp.dependencycheck.analyzer.NuspecAnalyzer.DEPENDENCY_ECOSYSTEM;

/**
 * Analyzes MS Project files for dependencies.
 *
 * @author Paul Irwin
 */
@ThreadSafe
@Experimental
public class MSBuildProjectAnalyzer extends AbstractFileTypeAnalyzer {

    /**
     * The logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(NuspecAnalyzer.class);

    /**
     * The name of the analyzer.
     */
    private static final String ANALYZER_NAME = "MSBuild Project Analyzer";

    /**
     * The phase in which the analyzer runs.
     */
    private static final AnalysisPhase ANALYSIS_PHASE = AnalysisPhase.INFORMATION_COLLECTION;

    /**
     * The types of files on which this will work.
     */
    private static final String[] SUPPORTED_EXTENSIONS = new String[]{"csproj", "vbproj"};

    /**
     * The file filter used to determine which files this analyzer supports.
     */
    private static final FileFilter FILTER = FileFilterBuilder.newInstance().addExtensions(SUPPORTED_EXTENSIONS).build();

    @Override
    public String getName() {
        return ANALYZER_NAME;
    }

    @Override
    public AnalysisPhase getAnalysisPhase() {
        return ANALYSIS_PHASE;
    }

    @Override
    protected FileFilter getFileFilter() {
        return FILTER;
    }

    @Override
    protected String getAnalyzerEnabledSettingKey() {
        return Settings.KEYS.ANALYZER_MSBUILD_PROJECT_ENABLED;
    }

    @Override
    protected void prepareFileTypeAnalyzer(Engine engine) throws InitializationException {
        // intentionally left blank
    }

    @Override
    protected void analyzeDependency(Dependency dependency, Engine engine) throws AnalysisException {
        LOGGER.debug("Checking MSBuild project file {}", dependency);
        try {
            final MSBuildProjectParser parser = new XPathMSBuildProjectParser();
            final List<NugetPackageReference> packages;

            try (FileInputStream fis = new FileInputStream(dependency.getActualFilePath())) {
                packages = parser.parse(fis);
            } catch (MSBuildProjectParseException | FileNotFoundException ex) {
                throw new AnalysisException(ex);
            }

            if (packages == null || packages.isEmpty()) {
                return;
            }

            for (NugetPackageReference npr : packages) {
                final Dependency child = new Dependency(dependency.getActualFile(), true);

                final String id = npr.getId();
                final String version = npr.getVersion();

                child.setEcosystem(DEPENDENCY_ECOSYSTEM);
                child.setName(id);
                child.setVersion(version);
                child.addEvidence(EvidenceType.PRODUCT, "msbuild", "id", id, Confidence.HIGHEST);
                child.addEvidence(EvidenceType.VERSION, "msbuild", "version", version, Confidence.HIGHEST);

                if (id.indexOf(".") > 0) {
                    final String[] parts = id.split("\\.");

                    // example: Microsoft.EntityFrameworkCore
                    child.addEvidence(EvidenceType.VENDOR, "msbuild", "id", parts[0], Confidence.MEDIUM);
                    child.addEvidence(EvidenceType.PRODUCT, "msbuild", "id", parts[1], Confidence.MEDIUM);

                    if (parts.length > 2) {
                        final String rest = id.substring(id.indexOf(".") + 1);
                        child.addEvidence(EvidenceType.PRODUCT, "msbuild", "id", rest, Confidence.MEDIUM);
                    }
                } else {
                    // example: jQuery
                    child.addEvidence(EvidenceType.VENDOR, "msbuild", "id", id, Confidence.LOW);
                }

                engine.addDependency(child);
            }

        } catch (Throwable e) {
            throw new AnalysisException(e);
        }
    }

}