class Snake:
    # list of coordinates
    body = []
    maze_width = 0
    maze_height = 0
    
    
    
    # head is of type Coordinate
    # width and height are integers
    def __init__(self, head, width, height):
        self.body.append(head)
        self.maze_width = width
        self.maze_height = height