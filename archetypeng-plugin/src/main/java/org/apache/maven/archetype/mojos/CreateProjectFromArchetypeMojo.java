/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.maven.archetype.mojos;

import org.apache.maven.archetype.ArchetypeGenerationRequest;
import org.apache.maven.archetype.ArchetypeGenerationResult;
import org.apache.maven.archetype.Archetype;
import org.apache.maven.archetype.common.ArchetypeRegistryManager;
import org.apache.maven.archetype.generator.ArchetypeGenerator;
import org.apache.maven.archetype.ui.ArchetypeGenerationConfigurator;
import org.apache.maven.archetype.ui.ArchetypeSelector;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.ContextEnabled;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.util.Arrays;
import java.util.Properties;
import org.apache.maven.execution.MavenSession;

/**
 * Generates sample project from archetype.
 *
 * @author rafale
 * @requiresProject false
 * @goal create
 * @execute phase="generate-sources"
 */
public class CreateProjectFromArchetypeMojo
    extends AbstractMojo
    implements ContextEnabled
{
    /** @component */
    private Archetype archetype;

    /** @component */
    private ArchetypeSelector selector;

    /** @component */
    ArchetypeRegistryManager archetypeRegistryManager;

    /** @component */
    ArchetypeGenerationConfigurator configurator;

    /** @component */
    ArchetypeGenerator generator;

    /** @component */
    private Invoker invoker;

    /**
     * The archetype's artifactId.
     *
     * @parameter expression="${archetypeArtifactId}"
     */
    private String archetypeArtifactId;

    /**
     * The archetype's groupId.
     *
     * @parameter expression="${archetypeGroupId}"
     */
    private String archetypeGroupId;

    /**
     * The archetype's version.
     *
     * @parameter expression="${archetypeVersion}"
     */
    private String archetypeVersion;

    /**
     * The archetype's repository.
     *
     * @parameter expression="${archetypeRepository}"
     */
    private String archetypeRepository;

    /**
     * The archetype's catalogs.
     * It is a comma separated list of catalogs.
     * Catalogs use scheme:
     * - 'file://...' with archetype-catalog.xml automatically appended when defining a directory
     * - 'http://...' with archetype-catalog.xml always appended
     * - 'local' which is the shortcut for 'file://~/.m2/archetype-catalog.xml'
     * - 'remote' which is the shortcut for 'http://repo1.maven.org/maven2'
     * - 'internal' which is an internal catalog
     *
     * @parameter expression="${archetypeCatalog}" default-value="internal"
     */
    private String archetypeCatalog;

    /**
     * Local maven repository.
     *
     * @parameter expression="${localRepository}"
     * @required
     * @readonly
     */
    private ArtifactRepository localRepository;

    /**
     * User settings use to check the interactiveMode.
     *
     * @parameter expression="${archetype.interactive}" default-value="${settings.interactiveMode}"
     * @required
     */
    private Boolean interactiveMode;

    /** @parameter expression="${basedir}" */
    private File basedir;

    /** 
     *  @parameter expression="${session}" 
     *  @readonly
     */
    private MavenSession session;
    /**
     * Additional goals that can be specified by the user during the creation of the archetype.
     *
     * @parameter expression="${goals}"
     */
    private String goals;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        
        Properties executionProperties = session.getExecutionProperties();
        
        ArchetypeGenerationRequest request = new ArchetypeGenerationRequest()
            .setArchetypeGroupId( archetypeGroupId )
            .setArchetypeArtifactId( archetypeArtifactId )
            .setArchetypeVersion( archetypeVersion )
            .setOutputDirectory( basedir.getAbsolutePath() )
            .setLocalRepository( localRepository )
            .setArchetypeRepository(archetypeRepository);

        try
        {
            selector.selectArchetype( request, interactiveMode, archetypeCatalog );

            configurator.configureArchetype( request, interactiveMode, executionProperties );

            ArchetypeGenerationResult result = archetype.generateProjectFromArchetype( request );
        }
        catch ( Exception ex )
        {
            throw new MojoExecutionException( ex.getMessage(), ex );
        }

        String artifactId = request.getArtifactId();

        String postArchetypeGenerationGoals = request.getArchetypeGoals();

        if ( StringUtils.isEmpty( postArchetypeGenerationGoals ) )
        {
            postArchetypeGenerationGoals = goals;
        }

        if ( StringUtils.isNotEmpty( postArchetypeGenerationGoals ) )
        {
            invokePostArchetypeGenerationGoals( postArchetypeGenerationGoals, artifactId );
        }
    }

    private void invokePostArchetypeGenerationGoals( String goals, String artifactId )
        throws MojoExecutionException, MojoFailureException
    {
        File projectBasedir = new File( basedir, artifactId );

        if ( projectBasedir.exists() )
        {
            InvocationRequest request = new DefaultInvocationRequest()
                .setBaseDirectory( projectBasedir )
                .setGoals( Arrays.asList( StringUtils.split( goals, "," ) ) );

            try
            {
                invoker.execute( request );
            }
            catch ( MavenInvocationException e )
            {
                throw new MojoExecutionException( "Cannot run additions goals." );
            }
        }
    }
}
