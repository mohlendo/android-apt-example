import android.app.Application;

import de.manuelohlendorf.androidaptexample.ComponentRegistry;

/**
 * Created by moh on 25.01.16.
 */
public class ExampleApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ComponentRegistry.getRegisteredClasses();
    }
}
