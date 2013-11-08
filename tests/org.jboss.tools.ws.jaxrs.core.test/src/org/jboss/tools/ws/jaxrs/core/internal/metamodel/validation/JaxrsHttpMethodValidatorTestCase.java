/******************************************************************************* 
 * Copyright (c) 2008 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Xavier Coulon - Initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.ws.jaxrs.core.internal.metamodel.validation;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.equalTo;
import static org.jboss.tools.ws.jaxrs.core.WorkbenchUtils.getAnnotation;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.validation.ValidationUtils.deleteJaxrsMarkers;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.validation.ValidationUtils.findJaxrsMarkers;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.validation.ValidationUtils.hasPreferenceKey;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.validation.ValidationUtils.toSet;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.HTTP_METHOD;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.RETENTION;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.TARGET;
import static org.jboss.tools.ws.jaxrs.core.preferences.JaxrsPreferences.HTTP_METHOD_INVALID_HTTP_METHOD_ANNOTATION_VALUE;
import static org.jboss.tools.ws.jaxrs.core.preferences.JaxrsPreferences.HTTP_METHOD_INVALID_RETENTION_ANNOTATION_VALUE;
import static org.jboss.tools.ws.jaxrs.core.preferences.JaxrsPreferences.HTTP_METHOD_INVALID_TARGET_ANNOTATION_VALUE;
import static org.jboss.tools.ws.jaxrs.core.preferences.JaxrsPreferences.HTTP_METHOD_MISSING_RETENTION_ANNOTATION;
import static org.jboss.tools.ws.jaxrs.core.preferences.JaxrsPreferences.HTTP_METHOD_MISSING_TARGET_ANNOTATION;
import static org.junit.Assert.assertThat;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.jdt.core.IType;
import org.eclipse.wst.validation.ReporterHelper;
import org.eclipse.wst.validation.internal.core.ValidationException;
import org.eclipse.wst.validation.internal.provisional.core.IReporter;
import org.jboss.tools.common.validation.ContextValidationHelper;
import org.jboss.tools.common.validation.IProjectValidationContext;
import org.jboss.tools.common.validation.ValidatorManager;
import org.jboss.tools.common.validation.internal.ProjectValidationContext;
import org.jboss.tools.ws.jaxrs.core.JBossJaxrsCorePlugin;
import org.jboss.tools.ws.jaxrs.core.WorkbenchUtils;
import org.jboss.tools.ws.jaxrs.core.builder.AbstractMetamodelBuilderTestCase;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsBaseElement;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsHttpMethod;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsEndpoint;
import org.jboss.tools.ws.jaxrs.core.preferences.JaxrsPreferences;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Xi
 * 
 */
@SuppressWarnings("restriction")
public class JaxrsHttpMethodValidatorTestCase extends AbstractMetamodelBuilderTestCase {

	private final IReporter reporter = new ReporterHelper(new NullProgressMonitor());
	private final ContextValidationHelper validationHelper = new ContextValidationHelper();
	private final IProjectValidationContext context = new ProjectValidationContext();
	private final ValidatorManager validatorManager = new ValidatorManager();

	@Before
	public void removeExtraJaxrsJavaApplications() throws CoreException {
		removeApplications(metamodel.getJavaApplications());
	}
	
	@After
	public void resetProblemLevelPreferences() {
		final IEclipsePreferences defaultPreferences = ((IScopeContext)DefaultScope.INSTANCE).getNode(JBossJaxrsCorePlugin.PLUGIN_ID);
		defaultPreferences.put(JaxrsPreferences.HTTP_METHOD_INVALID_RETENTION_ANNOTATION_VALUE, JaxrsPreferences.ERROR);
	}

	@Test
	public void shouldValidateHttpMethod() throws CoreException, ValidationException {
		// preconditions
		final IType fooType = getType("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		final JaxrsBaseElement httpMethod = (JaxrsBaseElement) metamodel.findElement(fooType);
		assertThat(findJaxrsMarkers(httpMethod).length, equalTo(0));
		deleteJaxrsMarkers(httpMethod);
		resetElementChangesNotifications();
		// operation
		new JaxrsMetamodelValidator().validate(toSet(httpMethod.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		// validation
		assertThat(findJaxrsMarkers(httpMethod).length, equalTo(0));
		for(IJaxrsEndpoint endpoint : metamodel.findEndpoints(httpMethod)) {
			assertThat(endpoint.getProblemLevel(), equalTo(0));
		}
		assertThat(metamodelProblemLevelChanges.size(), is(0));
	}

	@Test
	public void shouldSkipValidationOnBinaryHttpMethod() throws CoreException, ValidationException {
		// preconditions: create an HttpMethod from the binary annotation, then try to validate
		final IType getType = getType("javax.ws.rs.GET");
		final JaxrsHttpMethod httpMethod = JaxrsHttpMethod.from(getType).withMetamodel(metamodel).build();
		//metamodel.add(httpMethod);
		assertThat(findJaxrsMarkers(httpMethod).length, equalTo(0));
		deleteJaxrsMarkers(httpMethod);
		resetElementChangesNotifications();
		// operation
		new JaxrsMetamodelValidator().validate(toSet(httpMethod.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		// validation
		assertThat(findJaxrsMarkers(httpMethod).length, equalTo(0));
		assertThat(metamodelProblemLevelChanges.size(), is(0));
	}

	@Test
	public void shouldReportProblemWhenHttpMethodVerbIsEmpty() throws CoreException, ValidationException {
		// preconditions
		final IType fooType = getType("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		final JaxrsHttpMethod httpMethod = (JaxrsHttpMethod) metamodel.findElement(fooType);
		final Annotation httpMethodAnnotation = httpMethod.getAnnotation(HTTP_METHOD.qualifiedName);
		httpMethod.addOrUpdateAnnotation(createAnnotation(httpMethodAnnotation, new String()));
		deleteJaxrsMarkers(httpMethod);
		resetElementChangesNotifications();
		// operation
		new JaxrsMetamodelValidator().validate(toSet(httpMethod.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(httpMethod);
		assertThat(markers.length, equalTo(1));
		assertThat(markers, hasPreferenceKey(HTTP_METHOD_INVALID_HTTP_METHOD_ANNOTATION_VALUE));
		for(IJaxrsEndpoint endpoint : metamodel.findEndpoints(httpMethod)) {
			assertThat(endpoint.getProblemLevel(), not(equalTo(0)));
		}
		assertThat(metamodelProblemLevelChanges.contains(metamodel), is(true));
		assertThat(metamodelProblemLevelChanges.size(), is(1));
	}

	@Test
	public void shouldReportProblemWhenHttpMethodVerbIsNull() throws CoreException, ValidationException {
		// preconditions
		final IType fooType = getType("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		final JaxrsHttpMethod httpMethod = (JaxrsHttpMethod) metamodel.findElement(fooType);
		final Annotation httpMethodAnnotation = httpMethod.getAnnotation(HTTP_METHOD.qualifiedName);
		httpMethod.addOrUpdateAnnotation(createAnnotation(httpMethodAnnotation, (String) null));
		WorkbenchUtils.replaceFirstOccurrenceOfCode(fooType.getCompilationUnit(), "@HttpMethod(\"FOO\")", "@HttpMethod", true);
		deleteJaxrsMarkers(httpMethod);
		resetElementChangesNotifications();
		// operation
		new JaxrsMetamodelValidator().validate(toSet(httpMethod.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(httpMethod);
		assertThat(markers.length, equalTo(1));
		assertThat(markers, hasPreferenceKey(HTTP_METHOD_INVALID_HTTP_METHOD_ANNOTATION_VALUE));
		for(IJaxrsEndpoint endpoint : metamodel.findEndpoints(httpMethod)) {
			assertThat(endpoint.getProblemLevel(), not(equalTo(0)));
		}
		assertThat(metamodelProblemLevelChanges.contains(metamodel), is(true));
		assertThat(metamodelProblemLevelChanges.size(), is(1));
	}

	@Test
	public void shouldReportProblemWhenHttpMethodTypeMissesTargetAnnotation() throws CoreException, ValidationException {
		// preconditions
		final IType fooType = getType("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		final JaxrsHttpMethod httpMethod = (JaxrsHttpMethod) metamodel.findElement(fooType);
		final Annotation targetAnnotation = getAnnotation(fooType, TARGET.qualifiedName);
		httpMethod.removeAnnotation(targetAnnotation.getJavaAnnotation());
		WorkbenchUtils.replaceFirstOccurrenceOfCode(fooType.getCompilationUnit(), "@Target(value=ElementType.METHOD)", "", true);
		deleteJaxrsMarkers(httpMethod);
		resetElementChangesNotifications();
		// operation
		new JaxrsMetamodelValidator().validate(toSet(httpMethod.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(httpMethod);
		assertThat(markers.length, equalTo(1));
		assertThat(markers, hasPreferenceKey(HTTP_METHOD_MISSING_TARGET_ANNOTATION));
		for(IJaxrsEndpoint endpoint : metamodel.findEndpoints(httpMethod)) {
			assertThat(endpoint.getProblemLevel(), not(equalTo(0)));
		}
		assertThat(metamodelProblemLevelChanges.contains(metamodel), is(true));
		assertThat(metamodelProblemLevelChanges.size(), is(1));
	}

	@Test
	public void shouldReportProblemWhenHttpMethodTypeTargetAnnotationHasNullValue() throws CoreException,
			ValidationException {
		// preconditions
		final IType fooType = getType("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		final JaxrsHttpMethod httpMethod = (JaxrsHttpMethod) metamodel.findElement(fooType);
		final Annotation targetAnnotation = httpMethod.getAnnotation(TARGET.qualifiedName);
		httpMethod.addOrUpdateAnnotation(createAnnotation(targetAnnotation, (String) null));
		WorkbenchUtils.replaceFirstOccurrenceOfCode(fooType.getCompilationUnit(), "@Target(value=ElementType.METHOD)", "@Target", true);
		deleteJaxrsMarkers(httpMethod);
		resetElementChangesNotifications();
		// operation
		new JaxrsMetamodelValidator().validate(toSet(httpMethod.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(httpMethod);
		assertThat(markers.length, equalTo(1));
		assertThat(markers, hasPreferenceKey(HTTP_METHOD_INVALID_TARGET_ANNOTATION_VALUE));
		for(IJaxrsEndpoint endpoint : metamodel.findEndpoints(httpMethod)) {
			assertThat(endpoint.getProblemLevel(), not(equalTo(0)));
		}
		assertThat(metamodelProblemLevelChanges.contains(metamodel), is(true));
		assertThat(metamodelProblemLevelChanges.size(), is(1));
	}

	@Test
	public void shouldReportProblemWhenHttpMethodTypeTargetAnnotationHasWrongValue() throws CoreException,
			ValidationException {
		// preconditions
		final IType fooType = getType("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		final JaxrsHttpMethod httpMethod = (JaxrsHttpMethod) metamodel.findElement(fooType);
		final Annotation targetAnnotation = httpMethod.getAnnotation(TARGET.qualifiedName);
		httpMethod.addOrUpdateAnnotation(createAnnotation(targetAnnotation, "FOO"));
		WorkbenchUtils.replaceFirstOccurrenceOfCode(fooType.getCompilationUnit(), "@Target(value=ElementType.METHOD)", "@Target(value=ElementType.FIELD)", true);
		deleteJaxrsMarkers(httpMethod);
		resetElementChangesNotifications();
		// operation
		new JaxrsMetamodelValidator().validate(toSet(httpMethod.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(httpMethod);
		assertThat(markers.length, equalTo(1));
		assertThat(markers, hasPreferenceKey(HTTP_METHOD_INVALID_TARGET_ANNOTATION_VALUE));
		for(IJaxrsEndpoint endpoint : metamodel.findEndpoints(httpMethod)) {
			assertThat(endpoint.getProblemLevel(), not(equalTo(0)));
		}
		assertThat(metamodelProblemLevelChanges.contains(metamodel), is(true));
		assertThat(metamodelProblemLevelChanges.size(), is(1));
	}

	@Test
	public void shouldReportProblemWhenHttpMethodTypeMissesRetentionAnnotation() throws CoreException, ValidationException {
		// preconditions
		final IType fooType = getType("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		final JaxrsHttpMethod httpMethod = (JaxrsHttpMethod) metamodel.findElement(fooType);
		final Annotation targetAnnotation = getAnnotation(fooType, RETENTION.qualifiedName);
		httpMethod.removeAnnotation(targetAnnotation.getJavaAnnotation());
		WorkbenchUtils.replaceFirstOccurrenceOfCode(fooType.getCompilationUnit(), "@Retention(value=RetentionPolicy.RUNTIME)", "", true);
		deleteJaxrsMarkers(httpMethod);
		resetElementChangesNotifications();
		// operation
		new JaxrsMetamodelValidator().validate(toSet(httpMethod.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(httpMethod);
		assertThat(markers.length, equalTo(1));
		assertThat(markers, hasPreferenceKey(HTTP_METHOD_MISSING_RETENTION_ANNOTATION));
		for(IJaxrsEndpoint endpoint : metamodel.findEndpoints(httpMethod)) {
			assertThat(endpoint.getProblemLevel(), not(equalTo(0)));
		}
		assertThat(metamodelProblemLevelChanges.contains(metamodel), is(true));
		assertThat(metamodelProblemLevelChanges.size(), is(1));
	}
	
	@Test
	public void shouldReportProblemWhenHttpMethodTypeRetentionAnnotationHasNullValue() throws CoreException,
	ValidationException {
		// preconditions
		final IType fooType = getType("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		final JaxrsHttpMethod httpMethod = (JaxrsHttpMethod) metamodel.findElement(fooType);
		final Annotation retentionAnnotation = httpMethod.getAnnotation(RETENTION.qualifiedName);
		httpMethod.addOrUpdateAnnotation(createAnnotation(retentionAnnotation, (String)null));
		WorkbenchUtils.replaceFirstOccurrenceOfCode(fooType.getCompilationUnit(), "@Retention(value=RetentionPolicy.RUNTIME)", "@Retention", true);
		deleteJaxrsMarkers(httpMethod);
		resetElementChangesNotifications();
		// operation
		new JaxrsMetamodelValidator().validate(toSet(httpMethod.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(httpMethod);
		assertThat(markers.length, equalTo(1));
		assertThat(markers, hasPreferenceKey(HTTP_METHOD_INVALID_RETENTION_ANNOTATION_VALUE));
		for(IJaxrsEndpoint endpoint : metamodel.findEndpoints(httpMethod)) {
			assertThat(endpoint.getProblemLevel(), not(equalTo(0)));
		}
		assertThat(metamodelProblemLevelChanges.contains(metamodel), is(true));
		assertThat(metamodelProblemLevelChanges.size(), is(1));
	}
	
	@Test
	public void shouldReportProblemWhenHttpMethodTypeRetentionAnnotationHasWrongValue() throws CoreException,
		ValidationException {
		// preconditions
		final IType fooType = getType("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		final JaxrsHttpMethod httpMethod = (JaxrsHttpMethod) metamodel.findElement(fooType);
		final Annotation retentionAnnotation = httpMethod.getAnnotation(RETENTION.qualifiedName);
		httpMethod.addOrUpdateAnnotation(createAnnotation(retentionAnnotation, "FOO"));
		WorkbenchUtils.replaceFirstOccurrenceOfCode(fooType.getCompilationUnit(), "@Retention(value=RetentionPolicy.RUNTIME)", "@Retention(value=RetentionPolicy.SOURCE)", true);
		deleteJaxrsMarkers(httpMethod);
		resetElementChangesNotifications();
		// operation
		new JaxrsMetamodelValidator().validate(toSet(httpMethod.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(httpMethod);
		assertThat(markers.length, equalTo(1));
		assertThat(markers, hasPreferenceKey(HTTP_METHOD_INVALID_RETENTION_ANNOTATION_VALUE));
		for(IJaxrsEndpoint endpoint : metamodel.findEndpoints(httpMethod)) {
			assertThat(endpoint.getProblemLevel(), not(equalTo(0)));
		}
		assertThat(metamodelProblemLevelChanges.contains(metamodel), is(true));
		assertThat(metamodelProblemLevelChanges.size(), is(1));
	}


	/**
	 * @see 
	 * @throws CoreException
	 * @throws ValidationException
	 */
	@Test
	public void shouldNotReportProblemWhenRefactoringUnrelatedAnnotation() throws CoreException,
	ValidationException {
		// preconditions
		final IType customQualifierType = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomCDIQualifier");
		assertThat(customQualifierType.exists(), is(true));
		resetElementChangesNotifications();
		// operations: rename the Java type and attempt to create an HttpMethod and validate its underlying resource.
		customQualifierType.rename("FOOBAR", true, new NullProgressMonitor());
		final IType foobarType = getType("org.jboss.tools.ws.jaxrs.sample.services.FOOBAR");
		createHttpMethod(foobarType);
		new JaxrsMetamodelValidator().validate(toSet(foobarType.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		// validation
		final IMarker[] markers = foobarType.getResource().findMarkers(JaxrsMetamodelValidator.JAXRS_PROBLEM_MARKER_ID, false, IResource.DEPTH_INFINITE);
		assertThat(markers.length, equalTo(0));
	}
	
	@Test
	public void shouldNotFailOnProblemIfSeverityLevelIsIgnore() throws CoreException, ValidationException {
		// preconditions
		final IType fooType = getType("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		final JaxrsHttpMethod httpMethod = (JaxrsHttpMethod) metamodel.findElement(fooType);
		final Annotation retentionAnnotation = httpMethod.getAnnotation(RETENTION.qualifiedName);
		httpMethod.addOrUpdateAnnotation(createAnnotation(retentionAnnotation, "FOO"));
		WorkbenchUtils.replaceFirstOccurrenceOfCode(fooType.getCompilationUnit(), "@Retention(value=RetentionPolicy.RUNTIME)", "@Retention(value=RetentionPolicy.SOURCE)", true);
		deleteJaxrsMarkers(httpMethod);
		resetElementChangesNotifications();
		final IEclipsePreferences defaultPreferences = ((IScopeContext)DefaultScope.INSTANCE).getNode(JBossJaxrsCorePlugin.PLUGIN_ID);
		defaultPreferences.put(JaxrsPreferences.HTTP_METHOD_INVALID_RETENTION_ANNOTATION_VALUE, JaxrsPreferences.IGNORE);
		// operation
		new JaxrsMetamodelValidator().validate(toSet(httpMethod.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		// validation
		final IMarker[] markers = findJaxrsMarkers(httpMethod);
		assertThat(markers.length, equalTo(0));
	}
	
	@Test
	public void shouldIncreaseAndResetProblemLevelOnHttpMethod() throws CoreException, ValidationException {
		// preconditions
		final IType fooType = getType("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		final JaxrsHttpMethod httpMethod = (JaxrsHttpMethod) metamodel.findElement(fooType);
		final Annotation retentionAnnotation = httpMethod.getAnnotation(RETENTION.qualifiedName);
		httpMethod.addOrUpdateAnnotation(createAnnotation(retentionAnnotation, "FOO"));
		WorkbenchUtils.replaceFirstOccurrenceOfCode(fooType.getCompilationUnit(), "@Retention(value=RetentionPolicy.RUNTIME)", "@Retention(value=RetentionPolicy.SOURCE)", true);
		deleteJaxrsMarkers(httpMethod);
		resetElementChangesNotifications();
		// operation
		new JaxrsMetamodelValidator().validate(toSet(httpMethod.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		// verification: problem level is set to '2'
		assertThat(httpMethod.getProblemLevel(), equalTo(2));
		// now, fix the problem 
		WorkbenchUtils.replaceFirstOccurrenceOfCode(fooType.getCompilationUnit(), "@Retention(value=RetentionPolicy.SOURCE)", "@Retention(value=RetentionPolicy.RUNTIME)", true);
		// revalidate
		new JaxrsMetamodelValidator().validate(toSet(httpMethod.getResource()), project, validationHelper, context,
				validatorManager, reporter);
		// verification: problem level is set to '0'
		assertThat(httpMethod.getProblemLevel(), equalTo(0));
	}

}
