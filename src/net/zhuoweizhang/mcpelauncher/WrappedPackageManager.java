/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.zhuoweizhang.mcpelauncher;

import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.FeatureInfo;
import android.content.pm.InstrumentationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.content.pm.PermissionGroupInfo;
import android.content.pm.PermissionInfo;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;

import java.util.List;
import java.lang.reflect.Method;

/**
 * A mock {@link android.content.pm.PackageManager} class.  All methods are functional.
 * Override it to provide the operations that you need.
 */
public class WrappedPackageManager extends PackageManager {

    protected PackageManager wrapped;

    public WrappedPackageManager(PackageManager wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public PackageInfo getPackageInfo(String packageName, int flags)
    throws NameNotFoundException {
        return wrapped.getPackageInfo(packageName, flags);
    }

    @Override
    public String[] currentToCanonicalPackageNames(String[] names) {
        return wrapped.currentToCanonicalPackageNames(names);
    }
    
    @Override
    public String[] canonicalToCurrentPackageNames(String[] names) {
        return wrapped.canonicalToCurrentPackageNames(names);
    }
    
    @Override
    public Intent getLaunchIntentForPackage(String packageName) {
        return wrapped.getLaunchIntentForPackage(packageName);
    }

    @Override
    public int[] getPackageGids(String packageName) throws NameNotFoundException {
        return wrapped.getPackageGids(packageName);
    }

    @Override
    public PermissionInfo getPermissionInfo(String name, int flags)
    throws NameNotFoundException {
        return wrapped.getPermissionInfo(name, flags);
    }

    @Override
    public List<PermissionInfo> queryPermissionsByGroup(String group, int flags)
            throws NameNotFoundException {
        return wrapped.queryPermissionsByGroup(group, flags);
    }

    @Override
    public PermissionGroupInfo getPermissionGroupInfo(String name,
            int flags) throws NameNotFoundException {
        return wrapped.getPermissionGroupInfo(name, flags);
    }

    @Override
    public List<PermissionGroupInfo> getAllPermissionGroups(int flags) {
        return wrapped.getAllPermissionGroups(flags);
    }
    
    @Override
    public ApplicationInfo getApplicationInfo(String packageName, int flags)
    throws NameNotFoundException {
        return wrapped.getApplicationInfo(packageName, flags);
    }

    @Override
    public ActivityInfo getActivityInfo(ComponentName className, int flags)
    throws NameNotFoundException {
        return wrapped.getActivityInfo(className, flags);
    }

    @Override
    public ActivityInfo getReceiverInfo(ComponentName className, int flags)
    throws NameNotFoundException {
        return wrapped.getReceiverInfo(className, flags);
    }

    @Override
    public ServiceInfo getServiceInfo(ComponentName className, int flags)
    throws NameNotFoundException {
        return wrapped.getServiceInfo(className, flags);
    }

    @Override
    public ProviderInfo getProviderInfo(ComponentName className, int flags)
    throws NameNotFoundException {
        return wrapped.getProviderInfo(className, flags);
    }

    @Override
    public List<PackageInfo> getInstalledPackages(int flags) {
        return wrapped.getInstalledPackages(flags);
    }

    @Override
    public int checkPermission(String permName, String pkgName) {
        return wrapped.checkPermission(permName, pkgName);
    }

    @Override
    public boolean addPermission(PermissionInfo info) {
        return wrapped.addPermission(info);
    }

    @Override
    public boolean addPermissionAsync(PermissionInfo info) {
        return wrapped.addPermissionAsync(info);
    }

    @Override
    public void removePermission(String name) {
        wrapped.removePermission(name);
    }

    @Override
    public int checkSignatures(String pkg1, String pkg2) {
        return wrapped.checkSignatures(pkg1, pkg2);
    }

    @Override
    public int checkSignatures(int uid1, int uid2) {
        return wrapped.checkSignatures(uid1, uid2);
    }

    @Override
    public String[] getPackagesForUid(int uid) {
        return wrapped.getPackagesForUid(uid);
    }

    @Override
    public String getNameForUid(int uid) {
        return wrapped.getNameForUid(uid);
    }

    @Override
    public List<ApplicationInfo> getInstalledApplications(int flags) {
        return wrapped.getInstalledApplications(flags);
    }

    @Override
    public ResolveInfo resolveActivity(Intent intent, int flags) {
        return wrapped.resolveActivity(intent, flags);
    }

    @Override
    public List<ResolveInfo> queryIntentActivities(Intent intent, int flags) {
        return wrapped.queryIntentActivities(intent, flags);
    }

    @Override
    public List<ResolveInfo> queryIntentActivityOptions(ComponentName caller,
            Intent[] specifics, Intent intent, int flags) {
        return wrapped.queryIntentActivityOptions(caller, specifics, intent, flags);
    }

    @Override
    public List<ResolveInfo> queryBroadcastReceivers(Intent intent, int flags) {
        return wrapped.queryBroadcastReceivers(intent, flags);
    }

    @Override
    public ResolveInfo resolveService(Intent intent, int flags) {
        return wrapped.resolveService(intent, flags);
    }

    @Override
    public List<ResolveInfo> queryIntentServices(Intent intent, int flags) {
        return wrapped.queryIntentServices(intent, flags);
    }

    @Override
    public ProviderInfo resolveContentProvider(String name, int flags) {
        return wrapped.resolveContentProvider(name, flags);
    }

    @Override
    public List<ProviderInfo> queryContentProviders(String processName, int uid, int flags) {
        return wrapped.queryContentProviders(processName, uid, flags);
    }

    @Override
    public InstrumentationInfo getInstrumentationInfo(ComponentName className, int flags)
    throws NameNotFoundException {
        return wrapped.getInstrumentationInfo(className, flags);
    }

    @Override
    public List<InstrumentationInfo> queryInstrumentation(
            String targetPackage, int flags) {
        return wrapped.queryInstrumentation(targetPackage, flags);
    }

    @Override
    public Drawable getDrawable(String packageName, int resid, ApplicationInfo appInfo) {
        return wrapped.getDrawable(packageName, resid, appInfo);
    }

    @Override
    public Drawable getActivityIcon(ComponentName activityName)
    throws NameNotFoundException {
        return wrapped.getActivityIcon(activityName);
    }

    @Override
    public Drawable getActivityIcon(Intent intent) throws NameNotFoundException {
        return wrapped.getActivityIcon(intent);
    }

    @Override
    public Drawable getDefaultActivityIcon() {
        return wrapped.getDefaultActivityIcon();
    }

    @Override
    public Drawable getApplicationIcon(ApplicationInfo info) {
        return wrapped.getApplicationIcon(info);
    }

    @Override
    public Drawable getApplicationIcon(String packageName) throws NameNotFoundException {
        return wrapped.getApplicationIcon(packageName);
    }

    @Override
    public Drawable getActivityLogo(ComponentName activityName) throws NameNotFoundException {
        return wrapped.getActivityLogo(activityName);
    }

    @Override
    public Drawable getActivityLogo(Intent intent) throws NameNotFoundException {
        return wrapped.getActivityLogo(intent);
    }

    @Override
    public Drawable getApplicationLogo(ApplicationInfo info) {
        return wrapped.getApplicationLogo(info);
    }

    @Override
    public Drawable getApplicationLogo(String packageName) throws NameNotFoundException {
        return wrapped.getApplicationLogo(packageName);
    }

    @Override
    public CharSequence getText(String packageName, int resid, ApplicationInfo appInfo) {
        return wrapped.getText(packageName, resid, appInfo);
    }

    @Override
    public XmlResourceParser getXml(String packageName, int resid,
            ApplicationInfo appInfo) {
        return wrapped.getXml(packageName, resid, appInfo);
    }

    @Override
    public CharSequence getApplicationLabel(ApplicationInfo info) {
        return wrapped.getApplicationLabel(info);
    }

    @Override
    public Resources getResourcesForActivity(ComponentName activityName)
    throws NameNotFoundException {
        return wrapped.getResourcesForActivity(activityName);
    }

    @Override
    public Resources getResourcesForApplication(ApplicationInfo app) throws NameNotFoundException {
        return wrapped.getResourcesForApplication(app);
    }

    @Override
    public Resources getResourcesForApplication(String appPackageName)
    throws NameNotFoundException {
        return wrapped.getResourcesForApplication(appPackageName);
    }

    @Override
    public PackageInfo getPackageArchiveInfo(String archiveFilePath, int flags) {
        return wrapped.getPackageArchiveInfo(archiveFilePath, flags);
    }

    @Override
    public String getInstallerPackageName(String packageName) {
        return wrapped.getInstallerPackageName(packageName);
    }

    @Override
    public void addPackageToPreferred(String packageName) {
        wrapped.addPackageToPreferred(packageName);
    }

    @Override
    public void removePackageFromPreferred(String packageName) {
        wrapped.removePackageFromPreferred(packageName);
    }

    @Override
    public List<PackageInfo> getPreferredPackages(int flags) {
        return wrapped.getPreferredPackages(flags);
    }

    @Override
    public void setComponentEnabledSetting(ComponentName componentName,
            int newState, int flags) {
        wrapped.setComponentEnabledSetting(componentName, newState, flags);
    }

    @Override
    public int getComponentEnabledSetting(ComponentName componentName) {
        return wrapped.getComponentEnabledSetting(componentName);
    }

    @Override
    public void setApplicationEnabledSetting(String packageName, int newState, int flags) {
        wrapped.setApplicationEnabledSetting(packageName, newState, flags);
    }
    
    @Override
    public int getApplicationEnabledSetting(String packageName) {
        return wrapped.getApplicationEnabledSetting(packageName);
    }

    @Override
    public void addPreferredActivity(IntentFilter filter,
            int match, ComponentName[] set, ComponentName activity) {
        wrapped.addPreferredActivity(filter, match, set, activity);
    }

    @Override
    public void clearPackagePreferredActivities(String packageName) {
        wrapped.clearPackagePreferredActivities(packageName);
    }

    @Override
    public int getPreferredActivities(List<IntentFilter> outFilters,
            List<ComponentName> outActivities, String packageName) {
        return wrapped.getPreferredActivities(outFilters, outActivities, packageName);
    }
    
    @Override
    public String[] getSystemSharedLibraryNames() {
        return wrapped.getSystemSharedLibraryNames();
    }
    
    @Override
    public FeatureInfo[] getSystemAvailableFeatures() {
        return wrapped.getSystemAvailableFeatures();
    }
    
    @Override
    public boolean hasSystemFeature(String name) {
        return wrapped.hasSystemFeature(name);
    }
    
    @Override
    public boolean isSafeMode() {
        return wrapped.isSafeMode();
    }

	@Override
	public List<PackageInfo> getPackagesHoldingPermissions(String[] permissions, int flags) {
		return wrapped.getPackagesHoldingPermissions(permissions, flags);
	}

	@Override
	public List<ResolveInfo> queryIntentContentProviders(Intent intent, int flags) {
		return wrapped.queryIntentContentProviders(intent, flags);
	}

	@Override
	public void verifyPendingInstall(int id, int verificationCode) {
		wrapped.verifyPendingInstall(id, verificationCode);
	}

	@Override
	public void extendVerificationTimeout(int id, int verificationCodeAtTimeout,
			long millisecondsToDelay) {
		wrapped.extendVerificationTimeout(id, verificationCodeAtTimeout, millisecondsToDelay);
	}

	@Override
	public void setInstallerPackageName(String targetPackage, String installerPackageName) {
		wrapped.setInstallerPackageName(targetPackage, installerPackageName);
	}

    @Override
    public Drawable getActivityBanner(ComponentName activityName)
            throws NameNotFoundException {
        return wrapped.getActivityBanner(activityName);
    }

    @Override
    public Drawable getActivityBanner(Intent intent) throws NameNotFoundException {
        return wrapped.getActivityBanner(intent);
    }

    @Override
    public Drawable getApplicationBanner(ApplicationInfo info) {
        return wrapped.getApplicationBanner(info);
    }

    @Override
    public Drawable getApplicationBanner(String packageName) throws NameNotFoundException {
        return wrapped.getApplicationBanner(packageName);
    }

    @Override
    public Intent getLeanbackLaunchIntentForPackage(String packageName) {
        return wrapped.getLeanbackLaunchIntentForPackage(packageName);
    }

    @Override
    public PackageInstaller getPackageInstaller() {
        return wrapped.getPackageInstaller();
    }

    @Override
    public Drawable getUserBadgedIcon(Drawable icon, UserHandle user) {
        return wrapped.getUserBadgedIcon(icon, user);
    }

    @Override
    public Drawable getUserBadgedDrawableForDensity(Drawable drawable, UserHandle user,
            Rect badgeLocation,
            int badgeDensity) {
        return wrapped.getUserBadgedDrawableForDensity(drawable, user, badgeLocation, badgeDensity);
    }

    @Override
    public CharSequence getUserBadgedLabel(CharSequence label, UserHandle user) {
        return wrapped.getUserBadgedLabel(label, user);
    }

    @Override
    public boolean isPermissionRevokedByPolicy(String permName, String pkgName) {
        return wrapped.isPermissionRevokedByPolicy(permName, pkgName);
    }

    // Samsung Nougat ROMs use this
    public int getSystemFeatureLevel(String feature) {
        try {
            Method method = wrapped.getClass().getMethod("getSystemFeatureLevel", String.class);
            return (Integer)method.invoke(wrapped, feature);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
}
