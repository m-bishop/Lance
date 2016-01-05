package com.coredumpproject.pdemo;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;


/**
 * Created by Gregory on 1/4/2016.
 */

public class Player {
    static float WIDTH;
    static float HEIGHT;
    static float MAX_VELOCITY = 10f;
    static float JUMP_VELOCITY = 40f;
    static float DAMPING = 0.87f;

    enum State {
        Standing, Walking, Jumping
    }

    final Vector2 position = new Vector2();
    final Vector2 velocity = new Vector2();

    private State state = State.Walking;
    private PlatformDemoGame world;
    float stateTime = 0;

    private Texture playerTexture;
    private Animation stand;
    private Animation walk;
    private Animation jump;


    boolean facesRight = true;
    boolean grounded = false;

    public  Player(PlatformDemoGame newWorld){
        this.world = newWorld;

        playerTexture = new Texture("koalio.png");
        TextureRegion[] regions = TextureRegion.split(playerTexture, 18, 26)[0];
        stand = new Animation(0, regions[0]);
        jump = new Animation(0, regions[1]);
        walk = new Animation(0.15f, regions[2], regions[3], regions[4]);
        walk.setPlayMode(Animation.PlayMode.LOOP_PINGPONG);

        WIDTH = 1 / 16f * regions[0].getRegionWidth();
        HEIGHT = 1 / 16f * regions[0].getRegionHeight();
    }

    public void update (float deltaTime) {
        if (deltaTime == 0) return;
        this.stateTime += deltaTime;

        // check input and apply to velocity & state
        if ((Gdx.input.isKeyPressed(Input.Keys.SPACE) || isTouched(0.5f, 1)) && this.grounded) {
            this.velocity.y += JUMP_VELOCITY;
            this.setState(State.Jumping);
            this.grounded = false;
        }

        if (Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.A) || isTouched(0, 0.25f)) {
            this.velocity.x = -MAX_VELOCITY;
            if (this.grounded) this.state = State.Walking;
            this.facesRight = false;
        }

        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D) || isTouched(0.25f, 0.5f)) {
            this.velocity.x = MAX_VELOCITY;
            if (this.grounded) this.state = State.Walking;
            this.facesRight = true;
        }

        // apply gravity if we are falling
        this.velocity.add(0, world.getGravity());

        // clamp the velocity to the maximum, x-axis only
        this.velocity.x = MathUtils.clamp(this.velocity.x,
                -MAX_VELOCITY, MAX_VELOCITY);

        // If the velocity is < 1, set it to 0 and set state to Standing
        if (Math.abs(this.velocity.x) < 1) {
            this.velocity.x = 0;
            if (this.grounded) this.state = State.Standing;
        }

        // multiply by delta time so we know how far we go
        // in this frame
        this.velocity.scl(deltaTime);

        // perform collision detection & response, on each axis, separately
        // if the this is moving right, check the world.getTiles() to the right of it's
        // right bounding box edge, otherwise check the ones to the left
        Rectangle thisRect = world.getRectPool().obtain();
        thisRect.set(this.position.x, this.position.y, WIDTH, HEIGHT);
        int startX, startY, endX, endY;
        if (this.velocity.x > 0) {
            startX = endX = (int)(this.position.x + WIDTH + this.velocity.x);
        } else {
            startX = endX = (int)(this.position.x + this.velocity.x);
        }
        startY = (int)(this.position.y);
        endY = (int)(this.position.y + HEIGHT);
        getTiles(startX, startY, endX, endY);
        thisRect.x += this.velocity.x;
        for (Rectangle tile : world.getTiles()) {
            if (thisRect.overlaps(tile)) {
                this.velocity.x = 0;
                break;
            }
        }
        thisRect.x = this.position.x;

        // if the this is moving upwards, check the world.getTiles() to the top of its
        // top bounding box edge, otherwise check the ones to the bottom
        if (this.velocity.y > 0) {
            startY = endY = (int)(this.position.y + HEIGHT + this.velocity.y);
        } else {
            startY = endY = (int)(this.position.y + this.velocity.y);
        }
        startX = (int)(this.position.x);
        endX = (int)(this.position.x + WIDTH);
        getTiles(startX, startY, endX, endY);
        thisRect.y += this.velocity.y;
        for (Rectangle tile : world.getTiles()) {
            if (thisRect.overlaps(tile)) {
                // we actually reset the this y-position here
                // so it is just below/above the tile we collided with
                // this removes bouncing :)
                if (this.velocity.y > 0) {
                    this.position.y = tile.y - HEIGHT;
                    // we hit a block jumping upwards, let's destroy it!
                    TiledMapTileLayer layer = (TiledMapTileLayer) world.getMap().getLayers().get("walls");
                    layer.setCell((int)tile.x, (int)tile.y, null);
                } else {
                    this.position.y = tile.y + tile.height;
                    // if we hit the ground, mark us as grounded so we can jump
                    this.grounded = true;
                }
                this.velocity.y = 0;
                break;
            }
        }
        world.getRectPool().free(thisRect);

        // unscale the velocity by the inverse delta time and set
        // the latest position
        this.position.add(this.velocity);
        this.velocity.scl(1 / deltaTime);

        // Apply damping to the velocity on the x-axis so we don't
        // walk infinitely once a key was pressed
        this.velocity.x *= DAMPING;

    }

    private boolean isTouched (float startX, float endX) {
        // Check for touch inputs between startX and endX
        // startX/endX are given between 0 (left edge of the screen) and 1 (right edge of the screen)
        for (int i = 0; i < 2; i++) {
            float x = Gdx.input.getX(i) / (float)Gdx.graphics.getWidth();
            if (Gdx.input.isTouched(i) && (x >= startX && x <= endX)) {
                return true;
            }
        }
        return false;
    }

    private void getTiles (int startX, int startY, int endX, int endY) {
        TiledMapTileLayer layer = (TiledMapTileLayer) world.getMap().getLayers().get("walls");
        world.getRectPool().freeAll(world.getTiles());
        world.getTiles().clear();
        for (int y = startY; y <= endY; y++) {
            for (int x = startX; x <= endX; x++) {
                TiledMapTileLayer.Cell cell = layer.getCell(x, y);
                if (cell != null) {
                    Rectangle rect = world.getRectPool().obtain();
                    rect.set(x, y, 1, 1);
                    world.getTiles().add(rect);
                }
            }
        }
    }

    public Texture getPlayerTexture() {
        return playerTexture;
    }

    public Animation getStand() {
        return stand;
    }

    public Animation getWalk() {
        return walk;
    }

    public Animation getJump() {
        return jump;
    }

    public static float getWIDTH() {
        return WIDTH;
    }

    public static float getDAMPING() {
        return DAMPING;
    }

    public boolean isFacesRight() {
        return facesRight;
    }

    public boolean isGrounded() {
        return grounded;
    }

    public static float getHEIGHT() {
        return HEIGHT;
    }

    public static float getJumpVelocity() {
        return JUMP_VELOCITY;
    }

    public static float getMaxVelocity() {
        return MAX_VELOCITY;
    }

    public Vector2 getPosition() {
        return position;
    }

    public State getState() {
        return state;
    }

    public float getStateTime() {
        return stateTime;
    }

    public Vector2 getVelocity() {
        return velocity;
    }

    public void setFacesRight(boolean facesRight) {
        this.facesRight = facesRight;
    }

    public void setGrounded(boolean grounded) {
        this.grounded = grounded;
    }

    public void setState(State state) {
        this.state = state;
    }
}
