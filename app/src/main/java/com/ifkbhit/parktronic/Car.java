package com.ifkbhit.parktronic;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;

import java.util.ArrayList;

public class Car {


    private  Texture[]  texture;                                           //текстура машины
    private Line[]      lines_up = new Line[5], lines_down = new Line[5];  //опорные линии
    private Panel[]     p = new Panel[2];                                  //панель-индикатор
    private Point[][]   upper_dots = new Point[4][5];                      //верхние опорные точки
    private Point[][]   lower_dots = new Point[5][5];                      //нижние опорные точки(по идее не нужны)
    private Point[]     top_bumper = new Point[4];                         //примерные точки отслеживания на переднем бампере(по идее не нужны)
    private Point[]     down_bumper = new Point[4];                        //примерные точки отслеживания на заднем бампере
    // вот эти помогут
    private Point[]     up_supp_point = new Point[4];
    private Point[]     down_supp_point = new Point[4];
    private Line[]      support_line_up = new Line[4];
    private Line[]      support_line_down = new Line[4];
    public int          curTex = 0;
    private int         cur_panel = 0;

    public void changePanel(){
        cur_panel++;
    }

    double R0_up = 0, R0_down = 0, mid_lu = 0, mid_ld = 0; //среднее расстояния до "центра окружности" сверху и снизу,
                                                           // а так же средняя длинна отрезка сектора

    void response(Brick b, boolean isUp, Canvas canvas) {
        //Log.d("ANDREY", canvas.getWidth() / 2 + " " + upper_dots[0][2].x + " " + lower_dots[0][2].x);
        Paint p = new Paint();
        p.setColor(Color.RED);
        double MAX_PANEL_VAL = 0;
        double L, R;
        Point[] supp_point;
        Line[] supp_lines;

        //при любом значении isUp выполняются одинаковые действия, но для разных областей
        if (isUp) {  //если верхний блок
            b.refreshStates(lines_up);
            MAX_PANEL_VAL = 0.9;
            L = mid_lu;
            R = R0_up;
            supp_point = up_supp_point;
            supp_lines = support_line_up;
        }
        else {
            supp_point = down_supp_point;
            b.refreshStates(lines_down);
            MAX_PANEL_VAL = 1.1;
            L = mid_ld;
            R = R0_down;
            supp_lines = support_line_down;
        }

        ArrayList<Integer>[] states = b.getStates(); // получаем номера вершин в зонах
        if (Config.DEBUG_MOD) {
            String log = "";
            for (int i = 0; i < states.length; i++) {
                log += "{zone " + (i + 1) + " : ";
                if (states[i]!= null) {
                    if (!states[i].isEmpty()) {
                        for (Integer e : states[i]) {
                            log += e + " ";
                        }
                    }
                    else {log += "empty";}
                }
                else
                    log += "null";
                log += "} ";
            }
            canvas.drawText(log, 0, 20, p);
            canvas.drawText(b.toString(), 0, 40, p);
        }

        //для каждой точки зоны находим ее расстояние до машины

        double[] infoForPanel = {-2, -2, -2, -2};
        for (int i = 0; i < 4; i++) {
            if (!states[i].isEmpty()) {
                double min_l = 100;
                int j = 0;
                boolean flagUp = false;
                boolean flagDown = true;
                for (Integer point_index: states[i]) {
                    Point cur_point = b.points[point_index];
                    Line cur_supp_line = supp_lines[i];
                    Point intersectionPoint = cur_supp_line.intersectionPoint(new Line(cur_point, supp_point[i]));

                    double l = new Line(intersectionPoint, cur_point).getL();
                    if (isUp) {
                        if (cur_supp_line.isPointUpper(cur_point)) {
                            double x = MAX_PANEL_VAL * l / L;
                            if (x < min_l) {
                                min_l = x;
                            }
                        }
                        else {
                            flagUp = true;
                        }
                        j++;
                    }
                    else {
                        if (cur_supp_line.isPointUnder(cur_point)) {
                            double x = MAX_PANEL_VAL * l / L;
                            if (x < min_l) {
                                min_l = x;
                            }
                        }
                        else {
                            flagDown = true;
                            if(Config.DEBUG_MOD) {
                                canvas.drawText("Y_LINE: " + cur_supp_line.getY(cur_point.x) + " cur_point " + cur_point + " BOOLEAN " + ((cur_supp_line.getY(cur_point.x) <= cur_point.y) == supp_lines[i].isPointUnder(cur_point)), 0, 170 + 10 * j, new Paint());
                            }
                        }
                        j++;
                    }
                }
                String minimal = "NaN";
                if (isUp) {
                    if (!flagUp) {
                        if(min_l <= MAX_PANEL_VAL * 1.05) {
                            minimal = String.format("%.2f", min_l);
                        }
                        else {
                            min_l = -1;
                        }
                    }
                    else min_l = -1;
                }
                else {
                    boolean b1 = false;
                    for (Integer k: states[i]) {
                        if (supp_lines[i].isPointUpper(b.points[k])) {
                            b1 = true;
                        }
                    }
                    if (!b1) {
                        if (min_l <= 1.05 * MAX_PANEL_VAL) {
                            minimal = min_l + "";
                        }
                        else {
                            min_l = -1;
                        }
                    }
                    else min_l = -1;
                }
                infoForPanel[i] = min_l;
                if (Config.DEBUG_MOD) {
                    String inf = "";
                    for (int f = 0; f < 4; f++) {
                        inf += infoForPanel[f];
                    }
                    canvas.drawText("For zone " + (1 + i) + " min is " + infoForPanel, 0, 60 + 10 * i, p);
                }
                //infoForPanel.add(new Point(i, min_l ));
            }
            else infoForPanel[i] = -2;
        }
        if (Config.DEBUG_MOD) {
            String inf = "";
            for (int f = 0; f < 4; f++) {
                inf += "<" + infoForPanel[f] + "> ";
            }
            canvas.drawText(inf, 0, 120, p);
        }
        this.p[cur_panel%2].setPanel(infoForPanel, isUp);
        //return 0;
    }


    public Point getPos(){
        return texture[0].pos;
    }

    public Line[] getSupportLineUp() {
        return support_line_up;
    }

    public Line[] getSupportLineDown() {
        return support_line_down;
    }

    Car(Texture[] t, Canvas canvas, Resources res)
    {
        curTex = 0;
        p[0] = new Panel(canvas, res, false);
        p[1] = new Panel(canvas, res, true);

        Point tmpP;
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 5; j++) {
                tmpP = Config.UPPER_POINTS[i][j].multi(t[0].img.getHeight() / Config.CAR_H);
                upper_dots[i][j] = tmpP;
            }
        }
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                tmpP = Config.LOWER_POINTS[i][j].multi(t[0].img.getHeight() / Config.CAR_H);
                lower_dots[i][j] = tmpP;
            }
        }
        lines_up = new Line[5];
        lines_down = new Line[5];
        texture = t;

        for (int i = 0; i < 4; i++) {
            top_bumper[i] = upper_dots[3][i].medium(upper_dots[3][i+1]);
            down_bumper[i] = lower_dots[4][i].medium(lower_dots[4][i+1]);
        }

        for (int i = 0; i < 5; i++) {
            Log.d("CAR_CONSTRUCTOR", "LinesUp[" + i + "]");
            lines_up[i] = new Line(upper_dots[0][i].sum(texture[0].pos), upper_dots[3][i].sum(texture[0].pos));
            Log.d("CAR_CONSTRUCTOR", "LinesDown[" + i + "]");
            lines_down[i] = new Line(lower_dots[0][i].sum(texture[0].pos), lower_dots[4][i].sum(texture[0].pos));
        }

        for (int i = 0; i < 4; i++) {
            up_supp_point[i] = lines_up[i].intersectionPoint(lines_up[i + 1]);
            down_supp_point[i] = lines_down[i].intersectionPoint(lines_down[i + 1]);
            mid_lu += lines_up[i].getL();
            mid_ld += lines_down[i].getL();
        }

        if (Config.DEBUG_MOD) {
            for (int i = 0; i < down_supp_point.length; i++) {
                Log.d("CAR_CONSTRUCTOR", "up_supp_point[" + i + "] " + up_supp_point[i]);
            }
            for (int i = 0; i < down_supp_point.length; i++) {
                Log.d("CAR_CONSTRUCTOR", "down_supp_point[" + i + "] " + down_supp_point[i]);
            }
        }

        mid_lu += lines_up[4].getL();
        mid_ld += lines_down[4].getL();
        mid_lu /= 5.0;
        mid_ld /= 5.0;
        for (int i = 0; i < 4; i++) {
            Line maxDown =  new Line(lower_dots[0][i].sum(t[0].pos), down_supp_point[i]);
            R0_down += maxDown.getL();
            Line maxUp = new Line(upper_dots[0][i].sum(t[0].pos), up_supp_point[i]);
            R0_up += maxUp.getL();
        }

        R0_down /= 4.0;
        R0_up /= 4.0;

        R0_down -= mid_ld;
        R0_up -= mid_lu;

        for (int i = 0; i < 4; i++) {
            if (i == 0) {
                double x =0;
                Line tmp1 = new Line(lower_dots[4][i].sum(t[0].pos), lower_dots[4][i + 1].sum(t[0].pos));
                support_line_down[i] = new Line(new Point(x, tmp1.getY(x)), lower_dots[4][i + 1].sum(t[0].pos));

                Line tmp2 = new Line(upper_dots[3][i].sum(t[0].pos), upper_dots[3][i + 1].sum(t[0].pos));
                Log.d("CARAAA", "i is " + i + " " + tmp2.toString() + " first point " + new Point(x, tmp2.getY(x)));
                Log.d("CARAAA", tmp2.toString());
                support_line_up[i] =new Line(upper_dots[3][i+1].sum(t[0].pos),new Point(x, tmp2.getY(x)) );

                Log.d("CARAAA", support_line_up[i].toString());
            }
            else if (i == 3) {
                double x = canvas.getWidth();
                Line tmp1 = new Line(lower_dots[4][i].sum(t[0].pos), lower_dots[4][i+1].sum(t[0].pos));
                support_line_down[i] = new Line(new Point(x, tmp1.getY(x)), lower_dots[4][i].sum(t[0].pos));

                Line tmp2 = new Line(upper_dots[3][i].sum(t[0].pos), upper_dots[3][i + 1].sum(t[0].pos));
                support_line_up[i] =new Line(upper_dots[3][i].sum(t[0].pos),new Point(x, tmp2.getY(x)) );
            }
            else {
                support_line_down[i] = new Line(lower_dots[4][i].sum(t[0].pos), lower_dots[4][i + 1].sum(t[0].pos));
                support_line_up[i] = new Line(upper_dots[3][i].sum(t[0].pos), upper_dots[3][i + 1].sum(t[0].pos));
            }
        }
        Log.d("CARAAA", support_line_up[0].toString());
    }

    Point getUpper()
    {
        return upper_dots[3][2];
    }

    public Point[][] getUpper_dots() {
        return upper_dots;
    }

    public Point[][] getLower_dots() {
        return lower_dots;
    }

    Point getLower()
    {
        return lower_dots[4][2];
    }

    void setMovingResponse(boolean b)
    {
        p[cur_panel%2].setEmpty(b);
    }

    void drawCircle(Point center, Canvas c, int color) {
        Paint p = new Paint();
        p.setColor(color);
        c.drawCircle((float)center.x, (float)center.y, 4, p);
    }

    void draw(Canvas canvas) {
        Paint paint = new Paint();paint.setColor(Color.RED);
        texture[curTex].draw(canvas);
        p[cur_panel % 2].draw(canvas);
        if (Config.DEBUG_MOD) {
            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 5; j++) {
                    Point tmp = upper_dots[i][j];
                    canvas.drawCircle((float) (texture[0].pos.x + tmp.x), (float) (texture[0].pos.y + tmp.y), 4, paint);
                }
            }
            for (int i = 0; i < 5; i++) {
                for (int j = 0; j < 5; j++) {
                    Point tmp = lower_dots[i][j];
                    canvas.drawCircle((float) (texture[0].pos.x + tmp.x), (float) (texture[0].pos.y + tmp.y), 4, paint);
                }
            }
            for (int i = 0; i < 4; i++) {
                drawCircle(top_bumper[i].sum(texture[0].pos), canvas, Color.YELLOW);
                drawCircle(down_bumper[i].sum(texture[0].pos), canvas, Color.YELLOW);
            }
            for (int i = 0; i < 4; i++) {
                support_line_down[i].draw(canvas);
                support_line_up[i].draw(canvas);
            }
        }
    }
}