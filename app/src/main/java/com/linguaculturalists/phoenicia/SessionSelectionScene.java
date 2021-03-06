package com.linguaculturalists.phoenicia;

import com.linguaculturalists.phoenicia.components.Button;
import com.linguaculturalists.phoenicia.locale.Locale;
import com.linguaculturalists.phoenicia.locale.LocaleLoader;
import com.linguaculturalists.phoenicia.locale.LocaleManager;
import com.linguaculturalists.phoenicia.locale.Person;
import com.linguaculturalists.phoenicia.models.GameSession;
import com.linguaculturalists.phoenicia.util.GameFonts;
import com.linguaculturalists.phoenicia.util.PhoeniciaContext;
import com.orm.androrm.Filter;

import org.andengine.engine.handler.timer.ITimerCallback;
import org.andengine.engine.handler.timer.TimerHandler;
import org.andengine.entity.scene.Scene;
import org.andengine.entity.scene.background.Background;
import org.andengine.entity.sprite.ButtonSprite;
import org.andengine.entity.sprite.Sprite;
import org.andengine.entity.text.AutoWrap;
import org.andengine.entity.text.Text;
import org.andengine.entity.text.TextOptions;
import org.andengine.opengl.texture.TextureOptions;
import org.andengine.opengl.texture.bitmap.AssetBitmapTexture;
import org.andengine.opengl.texture.region.ITextureRegion;
import org.andengine.opengl.texture.region.TextureRegionFactory;
import org.andengine.util.adt.align.HorizontalAlign;
import org.andengine.util.adt.color.Color;
import org.andengine.util.debug.Debug;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Created by mhall on 6/2/15.
 */
public class SessionSelectionScene extends Scene {
    Sprite background;
    private AssetBitmapTexture backgroundTexture;
    private ITextureRegion backgroundTextureRegion;
    private PhoeniciaGame game;
    private SplashScene splash;

    public SessionSelectionScene(final PhoeniciaGame game, final SplashScene splash) {
        super();
        this.game = game;
        this.splash = splash;

        this.setBackground(new Background(new Color(100, 100, 100)));
        try {
            backgroundTexture = new AssetBitmapTexture(PhoeniciaContext.textureManager, PhoeniciaContext.assetManager, "textures/session_selection_background.png", TextureOptions.BILINEAR);
            backgroundTextureRegion = TextureRegionFactory.extractFromTexture(backgroundTexture);
            backgroundTexture.load();
            background = new Sprite(0, 0, backgroundTextureRegion, PhoeniciaContext.vboManager);

            //background.setScale(1.5f);
            background.setPosition(GameActivity.CAMERA_WIDTH/2, GameActivity.CAMERA_HEIGHT/2);
            this.attachChild(background);
        } catch (final IOException e) {
            System.err.println("Error loading background!");
            e.printStackTrace(System.err);
        }

        List<GameSession> sessions = GameSession.objects(PhoeniciaContext.context).all().toList();
        Debug.d("Number of Sessions: "+sessions.size());

        float startY = GameActivity.CAMERA_HEIGHT * 0.5f + 100;
        float startX = (GameActivity.CAMERA_WIDTH / 2) - ((sessions.size()-1) * 136);

        float offsetX = 0;
        for (int i = 0; i < sessions.size(); i++) {
            final GameSession session = sessions.get(i);
            if (session.session_name.get() == null) {
                session.session_name.set("Player "+(i+1));
            }
            LocaleLoader localeLoader = new LocaleLoader();
            Locale session_locale;
            try {
                session_locale = localeLoader.load(PhoeniciaContext.assetManager.open(session.locale_pack.get()));
            } catch (final IOException e) {
                Debug.e("Error loading Locale from "+session.locale_pack.get(), e);
                continue;
            }
            Person currentPerson = session_locale.person_map.get(session.person_name.get());
            if (currentPerson == null) {
                Debug.w("Game Session without person!");
                // TODO: use an "unknown user" image instead
                currentPerson = session_locale.people.get(i);
                session.person_name.set(currentPerson.name);
                session.save(PhoeniciaContext.context);
            }
            Debug.d("Adding Game session: " + session.session_name.get());
            try {
                AssetBitmapTexture texture = new AssetBitmapTexture(PhoeniciaContext.textureManager, PhoeniciaContext.assetManager, currentPerson.texture_src);
                texture.load();
                ITextureRegion personRegion = TextureRegionFactory.extractFromTexture(texture, 0, 0, game.PERSON_TILE_WIDTH, game.PERSON_TILE_HEIGHT);

                final ButtonSprite block = new ButtonSprite(startX + (272 * offsetX), startY, personRegion, PhoeniciaContext.vboManager);
                block.setOnClickListener(new ButtonSprite.OnClickListener() {
                    @Override
                    public void onClick(ButtonSprite buttonSprite, float v, float v2) {
                        Debug.d("Session " + session.session_name + " clicked");
                        startGame(session);
                    }
                });
                this.registerTouchArea(block);
                this.attachChild(block);

                Text personName = new Text(startX + (272 * offsetX), startY-128-16, GameFonts.dialogText(), session.session_name.get(), session.session_name.get().length(),  new TextOptions(AutoWrap.WORDS, 256, HorizontalAlign.CENTER), PhoeniciaContext.vboManager);
                this.attachChild(personName);
            } catch (final IOException e) {
                Debug.e("Error loading person sprite texture", e);
                continue;
            }
            offsetX++;

        }

        Button new_button = new Button(GameActivity.CAMERA_WIDTH/2, 100, 400, 100, "New Game", PhoeniciaContext.vboManager, new Button.OnClickListener() {
            @Override
            public void onClicked(Button button) {
                newGame();
            }
        });
        this.attachChild(new_button);
        this.registerTouchArea(new_button);
    }

    private void newGame() {
        this.detachSelf();
        game.activity.getEngine().setScene(new LocaleSelectionScene(this.game, this.splash));
    }

    private void startGame(final GameSession session) {
        this.detachSelf();
        game.activity.getEngine().setScene(splash);
        session.save(PhoeniciaContext.context);
        final SessionSelectionScene that = this;
        game.activity.getEngine().registerUpdateHandler(new TimerHandler(0.5f, new ITimerCallback() {
            public void onTimePassed(final TimerHandler pTimerHandler) {
            try {
                game.load(session);
                splash.detachSelf();
                game.activity.getEngine().setScene(game.scene);
                game.activity.getEngine().registerUpdateHandler(game);
                game.start();
            } catch (IOException e) {
                Debug.e("Failed to load game", e.getMessage());
                e.printStackTrace();
                splash.detachSelf();
                game.activity.getEngine().setScene(that);
            }
            }
        }));
    }
}
