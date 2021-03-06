/**
 * Mupen64PlusAE, an N64 emulator for the Android platform
 * 
 * Copyright (C) 2013 Paul Lamb
 * 
 * This file is part of Mupen64PlusAE.
 * 
 * Mupen64PlusAE is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * Mupen64PlusAE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Mupen64PlusAE. If
 * not, see <http://www.gnu.org/licenses/>.
 * 
 * Authors: littleguy77
 */
package paulscode.android.mupen64plusae.profile;

import paulscode.android.mupen64plusae.ActivityHelper;
import paulscode.android.mupen64plusae.persistent.GlobalPrefs;

public class ManageEmulationProfilesActivity extends ManageProfilesActivity
{
    @Override
    protected String getConfigFilePath( boolean isBuiltin )
    {
        return isBuiltin ? mAppData.emulationProfiles_cfg : mGlobalPrefs.emulationProfiles_cfg;
    }
    
    @Override
    protected String getNoDefaultProfile()
    {
        return GlobalPrefs.DEFAULT_EMULATION_PROFILE_DEFAULT;
    }
    
    @Override
    protected String getDefaultProfile()
    {
        return mGlobalPrefs.getEmulationProfileDefault();
    }
    
    @Override
    protected void putDefaultProfile( String name )
    {
        mGlobalPrefs.putEmulationProfileDefault( name );
    }
    
    @Override
    protected void onEditProfile( Profile profile )
    {
        ActivityHelper.startEmulationProfileActivity( this, profile.name );
    }
}
