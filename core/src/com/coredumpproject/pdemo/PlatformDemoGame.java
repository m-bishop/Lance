package com.coredumpproject.pdemo;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;


public class PlatformDemoGame extends InputAdapter implements ApplicationListener{
	/** The player character, has state and state time, */


	private OrthogonalTiledMapRenderer renderer;
	private OrthographicCamera camera;
	private Player koala;
	private Pool<Rectangle> rectPool = new Pool<Rectangle>() {
		@Override
		protected Rectangle newObject () {
			return new Rectangle();
		}
	};
	private Array<Rectangle> tiles = new Array<Rectangle>();
	private static final float GRAVITY = -2.5f;
	private TiledMap map;

	//TODO create world class, add these to it.

	public Array<Rectangle> getTiles() {
		return tiles;
	}

	public Pool<Rectangle> getRectPool() {
		return rectPool;
	}

	public float getGravity(){
		return GRAVITY;
	}

	public TiledMap getMap() {
		return map;
	}

	@Override
	public void create () {

		// load the map, set the unit scale to 1/16 (1 unit == 16 pixels)
		map = new TmxMapLoader().load("level1.tmx");
		renderer = new OrthogonalTiledMapRenderer(map, 1 / 16f);

		// create an orthographic camera, shows us 30x20 units of the world
		camera = new OrthographicCamera();
		camera.setToOrtho(false, 30, 20);
		camera.update();

		// create the Koala we want to move around the world
		koala = new Player(this);
		// koala = new Koala();
		koala.position.set(20, 20);
	}

	@Override
	public void resize(int width, int height) {

	}

	@Override
	public void render () {
		// clear the screen
		Gdx.gl.glClearColor(0.7f, 0.7f, 1.0f, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		// get the delta time
		float deltaTime = Gdx.graphics.getDeltaTime();

		// update the koala (process input, collision detection, position update)

		koala.update(deltaTime);


		// let the camera follow the koala, x-axis only
		camera.position.x = koala.position.x;
		camera.update();

		// set the TiledMapRenderer view based on what the
		// camera sees, and render the map
		renderer.setView(camera);
		renderer.render();

		// render the koala
		renderKoala();
	}

	@Override
	public void pause() {

	}

	@Override
	public void resume() {

	}


	private void renderKoala () {
		// based on the koala state, get the animation frame
		TextureRegion frame = null;
		switch (koala.getState()) {
			case Standing:
				frame = koala.getStand().getKeyFrame(koala.stateTime);
				break;
			case Walking:
				frame = koala.getWalk().getKeyFrame(koala.stateTime);
				break;
			case Jumping:
				frame = koala.getJump().getKeyFrame(koala.stateTime);
				break;
		}

		// draw the koala, depending on the current velocity
		// on the x-axis, draw the koala facing either right
		// or left
		Batch batch = renderer.getBatch();
		batch.begin();
		if (koala.facesRight) {
			batch.draw(frame, koala.position.x, koala.position.y, Player.WIDTH, Player.HEIGHT);
		} else {
			batch.draw(frame, koala.position.x + Player.WIDTH, koala.position.y, -Player.WIDTH, Player.HEIGHT);
		}
		batch.end();
	}

	@Override
	public void dispose () {
	}
}