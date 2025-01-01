package edu.curtin.saed.assignment2;

import javafx.scene.image.Image;

import java.io.InputStream;

public class GridAreaIcon
{
    private double x;
    private double y;
    private double rotation;
    private double scale;
    private InputStream imageStream;  // Keep this for the original constructor
    private Image image;  // Store the image directly
    private String caption;
    private boolean shown = true;

    // Existing constructor with InputStream
    public GridAreaIcon(double x, double y, double rotation, double scale, InputStream imageStream, String caption)
    {
        this.x = x;
        this.y = y;
        this.rotation = rotation;
        this.scale = scale;
        this.imageStream = imageStream;  // For lazy loading
        this.caption = caption;
    }

    // New constructor with Image directly
    public GridAreaIcon(double x, double y, double rotation, double scale, Image image, String caption)
    {
        this.x = x;
        this.y = y;
        this.rotation = rotation;
        this.scale = scale;
        this.image = image;  // Store the image directly
        this.caption = caption;
    }

    public double getX()
    {
        return x;
    }

    public double getY()
    {
        return y;
    }

    public double getRotation()
    {
        return rotation;
    }

    public double getScale()
    {
        return scale;
    }

    public Image getImage()
    {
        if (image == null && imageStream != null)
        {
            image = new Image(imageStream);  // Load image from stream if not already loaded
            imageStream = null;  // Release stream to save memory
        }
        return image;
    }

    public String getCaption()
    {
        return caption;
    }

    public boolean isShown()
    {
        return shown;
    }

    public void setPosition(double x, double y)
    {
        this.x = x;
        this.y = y;
    }

    public void setRotation(double rotation)
    {
        this.rotation = rotation;
    }

    public void setScale(double scale)
    {
        this.scale = scale;
    }

    public void setCaption(String caption)
    {
        this.caption = caption;
    }

    public void setShown(boolean shown)
    {
        this.shown = shown;
    }
}
